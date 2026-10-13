<?php

namespace App\Services\Import\Execution;

use App\Models\Customer;
use App\Models\Expense;
use App\Models\ImportJob;
use App\Models\ImportSource;
use App\Models\Order;
use App\Models\OrderItem;
use App\Models\Product;
use App\Models\Purchase;
use App\Models\PurchaseItem;
use App\Models\Store;
use App\Models\Supplier;
use App\Models\User;
use App\Models\Warehouse;
use App\Models\WarehouseStock;
use App\Services\ExpenseService;
use App\Services\InventoryService;
use App\Services\Import\ImportStateMachine;
use App\Services\Import\Normalizer\DataNormalizer;
use App\Services\Import\Reader\SpreadsheetReaderService;
use App\Support\Import\ImportStatus;
use App\Support\Import\ImportType;
use App\Support\Money;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Facades\Storage;
use Illuminate\Support\Str;
use InvalidArgumentException;
use RuntimeException;

class ImportExecutionService
{
    public const STRATEGY_SKIP = 'skip';
    public const STRATEGY_UPDATE = 'update';

    public function __construct(
        private SpreadsheetReaderService $readerService,
        private ImportStateMachine $stateMachine,
        private InventoryService $inventoryService,
        private ExpenseService $expenseService,
    ) {
    }

    /**
     * Execute user-confirmed import job.
     *
     * @param string $duplicateStrategy 'skip' or 'update'
     * @return array<string, mixed> Execution summary
     */
    public function executeJob(
        ImportJob $job,
        User $user,
        string $duplicateStrategy = self::STRATEGY_SKIP
    ): array {
        if (!in_array($duplicateStrategy, [self::STRATEGY_SKIP, self::STRATEGY_UPDATE], true)) {
            throw new InvalidArgumentException("راهبرد رفع تکرار '{$duplicateStrategy}' نامعتبر است.");
        }

        // Atomically lock job record to prevent concurrent execution races
        DB::transaction(function () use ($job) {
            $lockedJob = ImportJob::where('id', $job->id)->lockForUpdate()->firstOrFail();

            if ($lockedJob->status === ImportStatus::PREVIEW_READY) {
                $this->stateMachine->transition($lockedJob, ImportStatus::READY_TO_IMPORT, 'Ready to import confirmed');
            }

            if ($lockedJob->status !== ImportStatus::READY_TO_IMPORT) {
                throw new InvalidArgumentException("عملیات در وضعیت '{$lockedJob->status}' امکان اجرا ندارد.");
            }

            $this->stateMachine->transition($lockedJob, ImportStatus::IMPORTING, 'Import execution started');
        });

        $job->refresh();

        try {
            $source = $job->sources()->first();
            if (!$source) {
                throw new RuntimeException('فایل منبع یافت نشد.');
            }

            $storageDisk = Storage::disk('local');
            $absolutePath = $storageDisk->path($source->storage_path);

            $sheetIndex = (int) ($job->mapping_config['sheet_index'] ?? 0);
            $mapping = $job->mapping_config['fields'] ?? [];

            $result = match ($job->import_type) {
                ImportType::PRODUCTS => $this->executeProducts($job, $source, $absolutePath, $sheetIndex, $mapping, $duplicateStrategy),
                ImportType::CUSTOMERS => $this->executeCustomers($job, $source, $absolutePath, $sheetIndex, $mapping, $duplicateStrategy),
                ImportType::INVENTORY => $this->executeInventory($job, $source, $absolutePath, $sheetIndex, $mapping, $duplicateStrategy),
                ImportType::EXPENSES => $this->executeExpenses($job, $source, $absolutePath, $sheetIndex, $mapping, $duplicateStrategy, $user),
                ImportType::ORDERS => $this->executeOrders($job, $source, $absolutePath, $sheetIndex, $mapping, $duplicateStrategy, $user),
                ImportType::SUPPLIERS => $this->executeSuppliers($job, $source, $absolutePath, $sheetIndex, $mapping, $duplicateStrategy),
                ImportType::PURCHASES => $this->executePurchases($job, $source, $absolutePath, $sheetIndex, $mapping, $duplicateStrategy, $user),
                default => throw new InvalidArgumentException("نوع درون‌ریزی '{$job->import_type}' پشتیبانی نمی‌شود."),
            };

            $hasFailures = $result['failed_rows'] > 0;
            $finalStatus = $hasFailures ? ImportStatus::COMPLETED_WITH_ERRORS : ImportStatus::COMPLETED;

            $this->stateMachine->transition(
                $job,
                $finalStatus,
                "درون‌ریزی به پایان رسید: {$result['imported_rows']} رکورد درج/بروزرسانی شد، {$result['skipped_rows']} نادیده گرفته شد، {$result['failed_rows']} ناموفق."
            );

            return $result;

        } catch (\Throwable $e) {
            Log::error("Import execution failed for job {$job->id}: {$e->getMessage()}");
            $this->stateMachine->transition($job, ImportStatus::FAILED, 'خطا در اجرای درون‌ریزی: ' . $e->getMessage());
            throw $e;
        }
    }

    /**
     * Execute products import row-by-row in chunked transactions.
     * Strictly creates or updates Product records WITHOUT touching inventory or ledger tables.
     */
    private function executeProducts(
        ImportJob $job,
        ImportSource $source,
        string $filePath,
        int $sheetIndex,
        array $mapping,
        string $duplicateStrategy
    ): array {
        $seenSkusInFile = [];
        $importedRows = 0;
        $skippedRows = 0;
        $failedRows = 0;

        $batch = [];
        $batchSize = 100;

        $rowGenerator = $this->readerService->streamRows($filePath, $source->file_format, $sheetIndex);

        foreach ($rowGenerator as $rowItem) {
            $batch[] = $rowItem;
            if (count($batch) >= $batchSize) {
                $this->processProductBatch(
                    $batch,
                    $job,
                    $mapping,
                    $duplicateStrategy,
                    $seenSkusInFile,
                    $importedRows,
                    $skippedRows,
                    $failedRows
                );
                $batch = [];
            }
        }

        if (!empty($batch)) {
            $this->processProductBatch(
                $batch,
                $job,
                $mapping,
                $duplicateStrategy,
                $seenSkusInFile,
                $importedRows,
                $skippedRows,
                $failedRows
            );
        }

        $job->update([
            'imported_rows' => $importedRows,
            'skipped_rows' => $skippedRows,
            'failed_rows' => $failedRows,
        ]);

        return [
            'imported_rows' => $importedRows,
            'skipped_rows' => $skippedRows,
            'failed_rows' => $failedRows,
        ];
    }

    private function processProductBatch(
        array $batch,
        ImportJob $job,
        array $mapping,
        string $duplicateStrategy,
        array &$seenSkusInFile,
        int &$importedRows,
        int &$skippedRows,
        int &$failedRows
    ): void {
        DB::transaction(function () use (
            $batch,
            $job,
            $mapping,
            $duplicateStrategy,
            &$seenSkusInFile,
            &$importedRows,
            &$skippedRows,
            &$failedRows
        ) {
            foreach ($batch as $rowItem) {
                $nameRaw = $this->getMappedValue($rowItem, $mapping, 'name');
                $skuRaw = $this->getMappedValue($rowItem, $mapping, 'sku');
                $priceRaw = $this->getMappedValue($rowItem, $mapping, 'price');
                $costPriceRaw = $this->getMappedValue($rowItem, $mapping, 'cost_price');
                $unitRaw = $this->getMappedValue($rowItem, $mapping, 'unit');
                $barcodeRaw = $this->getMappedValue($rowItem, $mapping, 'barcode');
                $categoryRaw = $this->getMappedValue($rowItem, $mapping, 'category');
                $descriptionRaw = $this->getMappedValue($rowItem, $mapping, 'description');

                $name = DataNormalizer::cleanString($nameRaw);
                $sku = DataNormalizer::cleanString(DataNormalizer::toEnglishDigits($skuRaw ?? ''));
                $barcode = DataNormalizer::cleanString(DataNormalizer::toEnglishDigits($barcodeRaw ?? ''));
                $category = DataNormalizer::cleanString($categoryRaw);
                $description = DataNormalizer::cleanString($descriptionRaw);

                $price = DataNormalizer::parseMoney($priceRaw);
                $costPrice = ($costPriceRaw !== null && trim($costPriceRaw) !== '')
                    ? DataNormalizer::parseMoney($costPriceRaw)
                    : '0.00';
                $unit = DataNormalizer::normalizeUnit($unitRaw);

                // If invalid required fields, fail row
                if ($name === '' || $sku === '' || $price === null || $costPrice === null || $unit === null) {
                    $failedRows++;
                    continue;
                }

                // Check in-file duplicate SKU: skip subsequent rows
                $skuLower = mb_strtolower($sku);
                if (isset($seenSkusInFile[$skuLower])) {
                    $skippedRows++;
                    continue;
                }
                $seenSkusInFile[$skuLower] = true;

                // Check database match scoped to organization
                $existing = Product::where('organization_id', $job->organization_id)
                    ->where(function ($q) use ($sku, $barcode) {
                        $q->where('sku', $sku);
                        if ($barcode !== '') {
                            $q->orWhere('barcode', $barcode);
                        }
                    })
                    ->first();

                if ($existing) {
                    if ($duplicateStrategy === self::STRATEGY_SKIP) {
                        $skippedRows++;
                        continue;
                    }

                    // Update existing
                    $existing->update([
                        'name' => $name,
                        'price' => $price,
                        'cost_price' => $costPrice,
                        'unit' => $unit,
                        'barcode' => $barcode ?: $existing->barcode,
                        'category' => $category ?: $existing->category,
                        'description' => $description ?: $existing->description,
                    ]);
                    $importedRows++;
                } else {
                    // Create new product (Pure product catalog insertion, NO inventory/stock alteration)
                    Product::create([
                        'id' => (string) Str::uuid(),
                        'organization_id' => $job->organization_id,
                        'sku' => $sku,
                        'barcode' => $barcode ?: '',
                        'name' => $name,
                        'price' => $price,
                        'cost_price' => $costPrice,
                        'unit' => $unit,
                        'category' => $category ?: 'عمومی',
                        'description' => $description ?: null,
                    ]);
                    $importedRows++;
                }
            }
        });
    }

    /**
     * Execute customers import row-by-row in chunked transactions.
     */
    private function executeCustomers(
        ImportJob $job,
        ImportSource $source,
        string $filePath,
        int $sheetIndex,
        array $mapping,
        string $duplicateStrategy
    ): array {
        $seenPhonesInFile = [];
        $seenEmailsInFile = [];
        $importedRows = 0;
        $skippedRows = 0;
        $failedRows = 0;

        $batch = [];
        $batchSize = 100;

        $rowGenerator = $this->readerService->streamRows($filePath, $source->file_format, $sheetIndex);

        foreach ($rowGenerator as $rowItem) {
            $batch[] = $rowItem;
            if (count($batch) >= $batchSize) {
                $this->processCustomerBatch(
                    $batch,
                    $job,
                    $mapping,
                    $duplicateStrategy,
                    $seenPhonesInFile,
                    $seenEmailsInFile,
                    $importedRows,
                    $skippedRows,
                    $failedRows
                );
                $batch = [];
            }
        }

        if (!empty($batch)) {
            $this->processCustomerBatch(
                $batch,
                $job,
                $mapping,
                $duplicateStrategy,
                $seenPhonesInFile,
                $seenEmailsInFile,
                $importedRows,
                $skippedRows,
                $failedRows
            );
        }

        $job->update([
            'imported_rows' => $importedRows,
            'skipped_rows' => $skippedRows,
            'failed_rows' => $failedRows,
        ]);

        return [
            'imported_rows' => $importedRows,
            'skipped_rows' => $skippedRows,
            'failed_rows' => $failedRows,
        ];
    }

    private function processCustomerBatch(
        array $batch,
        ImportJob $job,
        array $mapping,
        string $duplicateStrategy,
        array &$seenPhonesInFile,
        array &$seenEmailsInFile,
        int &$importedRows,
        int &$skippedRows,
        int &$failedRows
    ): void {
        DB::transaction(function () use (
            $batch,
            $job,
            $mapping,
            $duplicateStrategy,
            &$seenPhonesInFile,
            &$seenEmailsInFile,
            &$importedRows,
            &$skippedRows,
            &$failedRows
        ) {
            foreach ($batch as $rowItem) {
                $nameRaw = $this->getMappedValue($rowItem, $mapping, 'name');
                $phoneRaw = $this->getMappedValue($rowItem, $mapping, 'phone');
                $emailRaw = $this->getMappedValue($rowItem, $mapping, 'email');
                $addressRaw = $this->getMappedValue($rowItem, $mapping, 'address');

                $name = DataNormalizer::cleanString($nameRaw);
                $phone = DataNormalizer::normalizePhone($phoneRaw);
                $email = DataNormalizer::normalizeEmail($emailRaw);
                $address = DataNormalizer::cleanString($addressRaw);

                // Name is required
                if ($name === '') {
                    $failedRows++;
                    continue;
                }

                // If phone was provided but invalid format, fail row
                if ($phoneRaw !== null && trim($phoneRaw) !== '' && $phone === null) {
                    $failedRows++;
                    continue;
                }

                // If email was provided but invalid format, fail row
                if ($emailRaw !== null && trim($emailRaw) !== '' && $email === null) {
                    $failedRows++;
                    continue;
                }

                // In-file duplicate check
                if ($phone && isset($seenPhonesInFile[$phone])) {
                    $skippedRows++;
                    continue;
                }
                if ($email && isset($seenEmailsInFile[$email])) {
                    $skippedRows++;
                    continue;
                }

                if ($phone) {
                    $seenPhonesInFile[$phone] = true;
                }
                if ($email) {
                    $seenEmailsInFile[$email] = true;
                }

                // Existing match in DB
                $existing = null;
                if ($phone) {
                    $existing = Customer::where('organization_id', $job->organization_id)
                        ->where('phone', $phone)
                        ->first();
                }
                if (!$existing && $email) {
                    $existing = Customer::where('organization_id', $job->organization_id)
                        ->where('email', $email)
                        ->first();
                }

                if ($existing) {
                    if ($duplicateStrategy === self::STRATEGY_SKIP) {
                        $skippedRows++;
                        continue;
                    }

                    // Update existing customer
                    $existing->update([
                        'name' => $name,
                        'phone' => $phone ?: $existing->phone,
                        'email' => $email ?: $existing->email,
                        'address' => $address ?: $existing->address,
                    ]);
                    $importedRows++;
                } else {
                    // Create new customer
                    Customer::create([
                        'id' => (string) Str::uuid(),
                        'organization_id' => $job->organization_id,
                        'name' => $name,
                        'phone' => $phone ?: null,
                        'email' => $email ?: null,
                        'address' => $address ?: null,
                        'total_purchases' => '0.00',
                        'loyalty_points' => 0,
                    ]);
                    $importedRows++;
                }
            }
        });
    }

    /**
     * Execute inventory opening balance import row-by-row in chunked transactions.
     * Strictly modifies warehouse_stock and stock_movements ledger via InventoryService.
     * Never creates fake sales, purchases, orders, expenses, or new products.
     */
    private function executeInventory(
        ImportJob $job,
        ImportSource $source,
        string $filePath,
        int $sheetIndex,
        array $mapping,
        string $duplicateStrategy
    ): array {
        $seenItemWarehouseInFile = [];
        $importedRows = 0;
        $skippedRows = 0;
        $failedRows = 0;

        $batch = [];
        $batchSize = 100;

        $defaultWarehouseId = $job->mapping_config['warehouse_id'] ?? null;
        $tenantWarehouses = Warehouse::where('organization_id', $job->organization_id)->get();
        $defaultWarehouse = $defaultWarehouseId ? $tenantWarehouses->firstWhere('id', $defaultWarehouseId) : null;

        $rowGenerator = $this->readerService->streamRows($filePath, $source->file_format, $sheetIndex);

        foreach ($rowGenerator as $rowItem) {
            $batch[] = $rowItem;
            if (count($batch) >= $batchSize) {
                $this->processInventoryBatch(
                    $batch,
                    $job,
                    $mapping,
                    $duplicateStrategy,
                    $tenantWarehouses,
                    $defaultWarehouse,
                    $seenItemWarehouseInFile,
                    $importedRows,
                    $skippedRows,
                    $failedRows
                );
                $batch = [];
            }
        }

        if (!empty($batch)) {
            $this->processInventoryBatch(
                $batch,
                $job,
                $mapping,
                $duplicateStrategy,
                $tenantWarehouses,
                $defaultWarehouse,
                $seenItemWarehouseInFile,
                $importedRows,
                $skippedRows,
                $failedRows
            );
        }

        $job->update([
            'imported_rows' => $importedRows,
            'skipped_rows' => $skippedRows,
            'failed_rows' => $failedRows,
        ]);

        return [
            'imported_rows' => $importedRows,
            'skipped_rows' => $skippedRows,
            'failed_rows' => $failedRows,
        ];
    }

    private function processInventoryBatch(
        array $batch,
        ImportJob $job,
        array $mapping,
        string $duplicateStrategy,
        $tenantWarehouses,
        ?Warehouse $defaultWarehouse,
        array &$seenItemWarehouseInFile,
        int &$importedRows,
        int &$skippedRows,
        int &$failedRows
    ): void {
        DB::transaction(function () use (
            $batch,
            $job,
            $mapping,
            $duplicateStrategy,
            $tenantWarehouses,
            $defaultWarehouse,
            &$seenItemWarehouseInFile,
            &$importedRows,
            &$skippedRows,
            &$failedRows
        ) {
            foreach ($batch as $rowItem) {
                $skuRaw = $this->getMappedValue($rowItem, $mapping, 'sku');
                $barcodeRaw = $this->getMappedValue($rowItem, $mapping, 'barcode');
                $quantityRaw = $this->getMappedValue($rowItem, $mapping, 'quantity');
                $warehouseRaw = $this->getMappedValue($rowItem, $mapping, 'warehouse');
                $unitCostRaw = $this->getMappedValue($rowItem, $mapping, 'unit_cost');
                $notesRaw = $this->getMappedValue($rowItem, $mapping, 'notes');

                $sku = DataNormalizer::cleanString(DataNormalizer::toEnglishDigits($skuRaw ?? ''));
                $barcode = DataNormalizer::cleanString(DataNormalizer::toEnglishDigits($barcodeRaw ?? ''));
                $warehouseVal = DataNormalizer::cleanString($warehouseRaw ?? '');
                $notes = DataNormalizer::cleanString($notesRaw ?? '');

                // 1. Resolve Product strictly within tenant (pre-existing only)
                $matchedProduct = null;
                if ($sku !== '' || $barcode !== '') {
                    $prodQuery = Product::where('organization_id', $job->organization_id);
                    if ($sku !== '' && $barcode !== '') {
                        $prodQuery->where(function ($q) use ($sku, $barcode) {
                            $q->where('sku', $sku)->orWhere('barcode', $barcode);
                        });
                    } elseif ($sku !== '') {
                        $prodQuery->where(function ($q) use ($sku) {
                            $q->where('sku', $sku)->orWhere('barcode', $sku);
                        });
                    } else {
                        $prodQuery->where('barcode', $barcode);
                    }

                    $candidates = $prodQuery->get();
                    if ($candidates->count() === 1) {
                        $matchedProduct = $candidates->first();
                    }
                }

                if (!$matchedProduct) {
                    $failedRows++;
                    continue;
                }

                // 2. Resolve Warehouse strictly within tenant
                $resolvedWarehouse = null;
                if ($warehouseVal !== '') {
                    $resolvedWarehouse = $tenantWarehouses->first(function ($wh) use ($warehouseVal) {
                        return mb_strtolower($wh->code ?? '') === mb_strtolower($warehouseVal)
                            || mb_strtolower($wh->name ?? '') === mb_strtolower($warehouseVal)
                            || $wh->id === $warehouseVal;
                    });
                }

                if (!$resolvedWarehouse) {
                    $resolvedWarehouse = $defaultWarehouse;
                }

                if (!$resolvedWarehouse) {
                    $failedRows++;
                    continue;
                }

                // 3. Parse and validate Quantity (positive integer)
                $qtyNorm = DataNormalizer::cleanString(DataNormalizer::toEnglishDigits($quantityRaw ?? ''));
                if ($qtyNorm === '' || !ctype_digit($qtyNorm) || (int) $qtyNorm <= 0) {
                    $failedRows++;
                    continue;
                }
                $targetQuantity = (int) $qtyNorm;

                // 4. Parse Unit Cost (optional)
                $parsedUnitCost = null;
                if ($unitCostRaw !== null && trim((string) $unitCostRaw) !== '') {
                    $parsedUnitCost = DataNormalizer::parseMoney($unitCostRaw);
                }

                // 5. In-file duplicate guard
                $pairKey = "{$matchedProduct->id}:{$resolvedWarehouse->id}";
                if (isset($seenItemWarehouseInFile[$pairKey])) {
                    $skippedRows++;
                    continue;
                }
                $seenItemWarehouseInFile[$pairKey] = true;

                // 6. Concurrency-safe stock resolution with pessimistic locking
                $existingStock = WarehouseStock::firstOrCreate(
                    [
                        'organization_id' => $job->organization_id,
                        'warehouse_id' => $resolvedWarehouse->id,
                        'product_id' => $matchedProduct->id,
                        'product_variant_id' => null,
                    ],
                    [
                        'id' => (string) Str::uuid(),
                        'quantity' => 0,
                        'reserved_quantity' => 0,
                    ]
                );

                $existingStock = WarehouseStock::where('id', $existingStock->id)
                    ->lockForUpdate()
                    ->first();

                $reason = $notes !== ''
                    ? "موجودی اولیه: {$notes}"
                    : 'موجودی اولیه (درون‌ریزی)';

                if ($existingStock && $existingStock->quantity > 0) {
                    if ($duplicateStrategy === self::STRATEGY_SKIP) {
                        $skippedRows++;
                        continue;
                    }

                    // In update mode: adjust delta to reach exact target opening balance
                    $currentQty = (int) $existingStock->quantity;
                    $delta = $targetQuantity - $currentQty;

                    if ($delta !== 0) {
                        $this->inventoryService->adjustStock(
                            $job->organization_id,
                            $resolvedWarehouse->id,
                            $matchedProduct->id,
                            null,
                            $delta,
                            $reason,
                            $job->id,
                            $parsedUnitCost
                        );
                    }
                    $importedRows++;
                } else {
                    // No existing balance or 0 quantity: create opening stock
                    $this->inventoryService->adjustStock(
                        $job->organization_id,
                        $resolvedWarehouse->id,
                        $matchedProduct->id,
                        null,
                        $targetQuantity,
                        $reason,
                        $job->id,
                        $parsedUnitCost
                    );
                    $importedRows++;
                }
            }
        });
    }

    /**
     * Execute operating expenses import.
     */
    private function executeExpenses(
        ImportJob $job,
        ImportSource $source,
        string $filePath,
        int $sheetIndex,
        array $mapping,
        string $duplicateStrategy,
        User $user
    ): array {
        $seenExpensesInFile = [];
        $importedRows = 0;
        $skippedRows = 0;
        $failedRows = 0;

        $batch = [];
        $batchSize = 100;

        $defaultStoreId = $job->mapping_config['store_id'] ?? null;
        $tenantStores = Store::where('organization_id', $job->organization_id)->get();
        $defaultStore = $defaultStoreId ? $tenantStores->firstWhere('id', $defaultStoreId) : $tenantStores->first();

        $rowGenerator = $this->readerService->streamRows($filePath, $source->file_format, $sheetIndex);

        foreach ($rowGenerator as $rowItem) {
            $batch[] = $rowItem;
            if (count($batch) >= $batchSize) {
                $this->processExpensesBatch(
                    $batch,
                    $job,
                    $mapping,
                    $duplicateStrategy,
                    $tenantStores,
                    $defaultStore,
                    $seenExpensesInFile,
                    $importedRows,
                    $skippedRows,
                    $failedRows,
                    $user
                );
                $batch = [];
            }
        }

        if (!empty($batch)) {
            $this->processExpensesBatch(
                $batch,
                $job,
                $mapping,
                $duplicateStrategy,
                $tenantStores,
                $defaultStore,
                $seenExpensesInFile,
                $importedRows,
                $skippedRows,
                $failedRows,
                $user
            );
        }

        $job->update([
            'imported_rows' => $importedRows,
            'skipped_rows' => $skippedRows,
            'failed_rows' => $failedRows,
        ]);

        return [
            'imported_rows' => $importedRows,
            'skipped_rows' => $skippedRows,
            'failed_rows' => $failedRows,
        ];
    }

    private function processExpensesBatch(
        array $batch,
        ImportJob $job,
        array $mapping,
        string $duplicateStrategy,
        $tenantStores,
        ?Store $defaultStore,
        array &$seenExpensesInFile,
        int &$importedRows,
        int &$skippedRows,
        int &$failedRows,
        User $user
    ): void {
        DB::transaction(function () use (
            $batch,
            $job,
            $mapping,
            $duplicateStrategy,
            $tenantStores,
            $defaultStore,
            &$seenExpensesInFile,
            &$importedRows,
            &$skippedRows,
            &$failedRows,
            $user
        ) {
            foreach ($batch as $rowItem) {
                $rowNumber = $rowItem['row_number'];
                $titleRaw = $this->getMappedValue($rowItem, $mapping, 'title');
                $categoryRaw = $this->getMappedValue($rowItem, $mapping, 'category');
                $amountRaw = $this->getMappedValue($rowItem, $mapping, 'amount');
                $dateRaw = $this->getMappedValue($rowItem, $mapping, 'date');
                $storeRaw = $this->getMappedValue($rowItem, $mapping, 'store');
                $paymentMethodRaw = $this->getMappedValue($rowItem, $mapping, 'payment_method');
                $notesRaw = $this->getMappedValue($rowItem, $mapping, 'notes');

                $title = DataNormalizer::cleanString($titleRaw ?? '');
                $categoryVal = DataNormalizer::cleanString($categoryRaw ?? '');
                $storeVal = DataNormalizer::cleanString($storeRaw ?? '');
                $notes = DataNormalizer::cleanString($notesRaw ?? '');
                $paymentMethod = DataNormalizer::cleanString($paymentMethodRaw ?? '') ?: 'CASH';

                // Seamless fallback: If title was not provided or empty, but notes was provided, use notes as title
                if ($title === '' && $notes !== '') {
                    $title = mb_substr($notes, 0, 191, 'UTF-8');
                }

                // 1. Title validation
                if ($title === '' || mb_strlen($title, 'UTF-8') > 191) {
                    $failedRows++;
                    continue;
                }

                // 2. Category validation & resolution
                $resolvedCategory = DataNormalizer::resolveExpenseCategory($categoryVal);
                if (!$resolvedCategory) {
                    $failedRows++;
                    continue;
                }

                // 3. Amount validation (exact money)
                $parsedAmount = DataNormalizer::parseMoney($amountRaw);
                if ($parsedAmount === null || (float) $parsedAmount <= 0) {
                    $failedRows++;
                    continue;
                }

                // 4. Date validation
                $parsedDate = DataNormalizer::parseDate($dateRaw);
                if ($parsedDate === null) {
                    $failedRows++;
                    continue;
                }

                // 5. Store resolution (strictly within tenant)
                $resolvedStore = null;
                if ($storeVal !== '') {
                    $resolvedStore = $tenantStores->first(function ($st) use ($storeVal) {
                        return mb_strtolower($st->code ?? '', 'UTF-8') === mb_strtolower($storeVal, 'UTF-8')
                            || mb_strtolower($st->name ?? '', 'UTF-8') === mb_strtolower($storeVal, 'UTF-8')
                            || $st->id === $storeVal;
                    });
                }
                if (!$resolvedStore) {
                    $resolvedStore = $defaultStore;
                }
                if (!$resolvedStore) {
                    $failedRows++;
                    continue;
                }

                // 6. In-file duplicate check
                $fileDupKey = "{$resolvedStore->id}_{$parsedDate}_{$resolvedCategory}_{$parsedAmount}_" . mb_strtolower($title, 'UTF-8');
                if (isset($seenExpensesInFile[$fileDupKey])) {
                    $skippedRows++;
                    continue;
                }
                $seenExpensesInFile[$fileDupKey] = $rowNumber;

                // 7. Stable per-row deterministic ID for idempotency & retry safety
                $deterministicId = ExpenseService::deterministicUuid("resto_imp_{$job->organization_id}_{$job->id}_{$rowNumber}");

                // Check if this row was already committed by this job (e.g. retry after partial failure)
                $alreadyCommitted = Expense::where('organization_id', $job->organization_id)
                    ->where('id', $deterministicId)
                    ->first();

                if ($alreadyCommitted) {
                    $skippedRows++;
                    continue;
                }

                // 8. Check for existing matching Expense record in database
                $existingExpense = Expense::where('organization_id', $job->organization_id)
                    ->where('store_id', $resolvedStore->id)
                    ->where('date', $parsedDate)
                    ->where('category', $resolvedCategory)
                    ->where('amount', $parsedAmount)
                    ->whereRaw('LOWER(title) = ?', [mb_strtolower($title, 'UTF-8')])
                    ->first();

                if ($existingExpense) {
                    if ($duplicateStrategy === self::STRATEGY_SKIP) {
                        $skippedRows++;
                        continue;
                    }

                    // UPDATE strategy: update optional fields (notes, payment_method)
                    $updatePayload = [];
                    if ($notes !== '') {
                        $updatePayload['notes'] = $notes;
                    }
                    if ($paymentMethod !== 'CASH') {
                        $updatePayload['payment_method'] = $paymentMethod;
                    }

                    if (!empty($updatePayload)) {
                        $this->expenseService->updateExpense($existingExpense, $updatePayload);
                    }
                    $importedRows++;
                    continue;
                }

                // 9. Create fresh Expense using canonical ExpenseService
                $this->expenseService->createExpense([
                    'id' => $deterministicId,
                    'organization_id' => $job->organization_id,
                    'store_id' => $resolvedStore->id,
                    'title' => $title,
                    'category' => $resolvedCategory,
                    'amount' => $parsedAmount,
                    'payment_method' => $paymentMethod,
                    'date' => $parsedDate,
                    'notes' => $notes,
                    'user_id' => $user->id,
                ], $user);

                $importedRows++;
            }
        });
    }

    /**
     * Execute historical orders import adhering strictly to HISTORICAL_RECORD_ONLY domain contract.
     * ZERO mutations to WarehouseStock, ZERO stock movements, ZERO live general ledger journal entries.
     */
    private function executeOrders(
        ImportJob $job,
        ImportSource $source,
        string $filePath,
        int $sheetIndex,
        array $mapping,
        string $duplicateStrategy,
        User $user
    ): array {
        $importedRows = 0;
        $skippedRows = 0;
        $failedRows = 0;

        $defaultStoreId = $job->mapping_config['store_id'] ?? null;
        $tenantStores = Store::where('organization_id', $job->organization_id)->get();
        $defaultStore = $defaultStoreId ? $tenantStores->firstWhere('id', $defaultStoreId) : $tenantStores->first();

        $defaultWarehouseId = $job->mapping_config['warehouse_id'] ?? null;
        $tenantWarehouses = Warehouse::where('organization_id', $job->organization_id)->get();
        $defaultWarehouse = $defaultWarehouseId ? $tenantWarehouses->firstWhere('id', $defaultWarehouseId) : $tenantWarehouses->first();

        $tenantProducts = Product::where('organization_id', $job->organization_id)->get();
        $tenantCustomers = Customer::where('organization_id', $job->organization_id)->get();

        // 1. Group rows by external order_id
        $groupedOrders = [];
        $rowGenerator = $this->readerService->streamRows($filePath, $source->file_format, $sheetIndex);

        foreach ($rowGenerator as $rowItem) {
            $rowNumber = $rowItem['row_number'];
            $orderIdRaw = $this->getMappedValue($rowItem, $mapping, 'order_id');
            $skuRaw = $this->getMappedValue($rowItem, $mapping, 'sku');
            $qtyRaw = $this->getMappedValue($rowItem, $mapping, 'quantity');
            $priceRaw = $this->getMappedValue($rowItem, $mapping, 'unit_price');
            $dateRaw = $this->getMappedValue($rowItem, $mapping, 'date');
            $discRaw = $this->getMappedValue($rowItem, $mapping, 'discount_amount');
            $taxRaw = $this->getMappedValue($rowItem, $mapping, 'tax_amount');
            $totalRaw = $this->getMappedValue($rowItem, $mapping, 'total_amount');
            $custRaw = $this->getMappedValue($rowItem, $mapping, 'customer');
            $storeRaw = $this->getMappedValue($rowItem, $mapping, 'store');
            $payRaw = $this->getMappedValue($rowItem, $mapping, 'payment_method');
            $statusRaw = $this->getMappedValue($rowItem, $mapping, 'status');
            $notesRaw = $this->getMappedValue($rowItem, $mapping, 'notes');

            $orderId = DataNormalizer::cleanString($orderIdRaw ?? '');
            $groupKey = ($orderId !== '') ? $orderId : ("__MISSING_ROW_{$rowNumber}");

            $groupedOrders[$groupKey][] = [
                'row_number' => $rowNumber,
                'order_id' => $orderId,
                'sku_raw' => $skuRaw,
                'qty_raw' => $qtyRaw,
                'price_raw' => $priceRaw,
                'date_raw' => $dateRaw,
                'disc_raw' => $discRaw,
                'tax_raw' => $taxRaw,
                'total_raw' => $totalRaw,
                'cust_raw' => $custRaw,
                'store_raw' => $storeRaw,
                'pay_raw' => $payRaw,
                'status_raw' => $statusRaw,
                'notes_raw' => $notesRaw,
            ];
        }

        // 2. Process each order group inside its own database transaction
        foreach ($groupedOrders as $groupKey => $orderRows) {
            $this->processOrderGroup(
                $orderRows,
                $job,
                $duplicateStrategy,
                $tenantStores,
                $defaultStore,
                $tenantWarehouses,
                $defaultWarehouse,
                $tenantProducts,
                $tenantCustomers,
                $user,
                $importedRows,
                $skippedRows,
                $failedRows
            );
        }

        $job->update([
            'imported_rows' => $importedRows,
            'skipped_rows' => $skippedRows,
            'failed_rows' => $failedRows,
        ]);

        return [
            'imported_rows' => $importedRows,
            'skipped_rows' => $skippedRows,
            'failed_rows' => $failedRows,
        ];
    }

    private function processOrderGroup(
        array $orderRows,
        ImportJob $job,
        string $duplicateStrategy,
        $tenantStores,
        ?Store $defaultStore,
        $tenantWarehouses,
        ?Warehouse $defaultWarehouse,
        $tenantProducts,
        $tenantCustomers,
        User $user,
        int &$importedRows,
        int &$skippedRows,
        int &$failedRows
    ): void {
        DB::transaction(function () use (
            $orderRows,
            $job,
            $duplicateStrategy,
            $tenantStores,
            $defaultStore,
            $tenantWarehouses,
            $defaultWarehouse,
            $tenantProducts,
            $tenantCustomers,
            $user,
            &$importedRows,
            &$skippedRows,
            &$failedRows
        ) {
            $firstRow = $orderRows[0];
            $externalOrderId = $firstRow['order_id'];

            if ($externalOrderId === '') {
                $failedRows += count($orderRows);
                return;
            }

            // Parse Date
            $parsedDate = DataNormalizer::parseDate($firstRow['date_raw']);
            if (!$parsedDate) {
                $failedRows += count($orderRows);
                return;
            }

            // Resolve Store
            $storeRaw = DataNormalizer::cleanString($firstRow['store_raw'] ?? '');
            $resolvedStore = null;
            if ($storeRaw !== '') {
                $resolvedStore = $tenantStores->first(function ($st) use ($storeRaw) {
                    return mb_strtolower($st->code ?? '', 'UTF-8') === mb_strtolower($storeRaw, 'UTF-8')
                        || mb_strtolower($st->name ?? '', 'UTF-8') === mb_strtolower($storeRaw, 'UTF-8')
                        || $st->id === $storeRaw;
                });
            }
            if (!$resolvedStore) {
                $resolvedStore = $defaultStore;
            }
            if (!$resolvedStore) {
                $failedRows += count($orderRows);
                return;
            }

            // Resolve Warehouse
            $resolvedWarehouse = $defaultWarehouse;

            // Resolve Customer
            $customerRaw = DataNormalizer::cleanString($firstRow['cust_raw'] ?? '');
            $resolvedCustomer = null;
            if ($customerRaw !== '') {
                $resolvedCustomer = $tenantCustomers->first(function ($c) use ($customerRaw) {
                    return $c->id === $customerRaw
                        || mb_strtolower($c->name ?? '', 'UTF-8') === mb_strtolower($customerRaw, 'UTF-8')
                        || ($c->phone && $c->phone === DataNormalizer::normalizePhone($customerRaw));
                });
            }

            $customerName = $resolvedCustomer ? $resolvedCustomer->name : ($customerRaw ?: 'مشتری حضوری');
            $customerId = $resolvedCustomer?->id;

            // Payment and Status
            $paymentMethod = DataNormalizer::cleanString($firstRow['pay_raw'] ?? '') ?: 'CASH';
            $statusRaw = strtoupper(DataNormalizer::cleanString($firstRow['status_raw'] ?? '') ?: 'COMPLETED');
            $paymentStatus = in_array($statusRaw, ['COMPLETED', 'PAID'], true) ? 'PAID' : 'PENDING';
            $fulfillmentStatus = in_array($statusRaw, ['COMPLETED'], true) ? 'COMPLETED' : ($statusRaw === 'CANCELLED' ? 'CANCELLED' : 'NEW');
            $notes = DataNormalizer::cleanString($firstRow['notes_raw'] ?? '');

            // Deterministic Order UUID based on tenant, job and external order id
            $orderDeterministicId = ExpenseService::deterministicUuid("resto_imp_{$job->organization_id}_{$job->id}_ord_{$externalOrderId}");

            // Check if this order was already committed by this job (retry idempotency)
            $alreadyCommittedByJob = Order::where('organization_id', $job->organization_id)
                ->where('id', $orderDeterministicId)
                ->first();

            if ($alreadyCommittedByJob) {
                $skippedRows += count($orderRows);
                return;
            }

            // Check for existing matching Order in database (by external_order_id or order_number)
            $existingOrder = Order::where('organization_id', $job->organization_id)
                ->where(function ($q) use ($externalOrderId) {
                    $q->where('external_order_id', $externalOrderId)
                      ->orWhere('order_number', $externalOrderId);
                })
                ->first();

            if ($existingOrder) {
                if ($duplicateStrategy === self::STRATEGY_SKIP) {
                    $skippedRows += count($orderRows);
                    return;
                }

                // UPDATE Strategy: only update descriptive non-financial attributes on historical order
                $updatePayload = [];
                if ($notes !== '') {
                    $updatePayload['notes'] = $notes;
                }
                if ($paymentMethod !== 'CASH') {
                    $updatePayload['payment_method'] = $paymentMethod;
                }
                if (!empty($updatePayload)) {
                    $existingOrder->update($updatePayload);
                }
                $importedRows += count($orderRows);
                return;
            }

            // Validate and prepare line items
            $preparedLines = [];
            $subtotalMinor = 0;
            $discountMinor = 0;
            $taxMinor = 0;

            foreach ($orderRows as $rowItem) {
                $skuNorm = DataNormalizer::cleanString(DataNormalizer::toEnglishDigits($rowItem['sku_raw'] ?? ''));
                $matchedProduct = $tenantProducts->first(function ($p) use ($skuNorm) {
                    return (string) $p->sku === $skuNorm
                        || (string) $p->barcode === $skuNorm
                        || (string) $p->id === $skuNorm;
                });

                if (!$matchedProduct) {
                    $failedRows += count($orderRows);
                    return;
                }

                $qtyNorm = DataNormalizer::cleanString(DataNormalizer::toEnglishDigits($rowItem['qty_raw'] ?? ''));
                if ($qtyNorm === '' || !ctype_digit($qtyNorm) || (int) $qtyNorm <= 0) {
                    $failedRows += count($orderRows);
                    return;
                }
                $qty = (int) $qtyNorm;

                $priceNorm = DataNormalizer::parseMoney($rowItem['price_raw']);
                if ($priceNorm === null || (float) $priceNorm < 0) {
                    $failedRows += count($orderRows);
                    return;
                }

                $unitPriceMinor = Money::toMinor($priceNorm);
                $lineSubtotalMinor = Money::multiply($unitPriceMinor, $qty);

                $lineDiscMinor = 0;
                if ($rowItem['disc_raw'] !== null && trim((string) $rowItem['disc_raw']) !== '') {
                    $discNorm = DataNormalizer::parseMoney($rowItem['disc_raw']);
                    if ($discNorm !== null) {
                        $lineDiscMinor = Money::toMinor($discNorm);
                    }
                }

                $lineTaxMinor = 0;
                if ($rowItem['tax_raw'] !== null && trim((string) $rowItem['tax_raw']) !== '') {
                    $taxNorm = DataNormalizer::parseMoney($rowItem['tax_raw']);
                    if ($taxNorm !== null) {
                        $lineTaxMinor = Money::toMinor($taxNorm);
                    }
                }

                $lineTotalMinor = $lineSubtotalMinor - $lineDiscMinor + $lineTaxMinor;

                $subtotalMinor += $lineSubtotalMinor;
                $discountMinor += $lineDiscMinor;
                $taxMinor += $lineTaxMinor;

                $lineDeterministicId = ExpenseService::deterministicUuid("resto_imp_{$job->organization_id}_{$job->id}_item_{$externalOrderId}_{$rowItem['row_number']}");

                $preparedLines[] = [
                    'id' => $lineDeterministicId,
                    'order_id' => $orderDeterministicId,
                    'product_id' => $matchedProduct->id,
                    'product_variant_id' => null,
                    'product_name' => $matchedProduct->name,
                    'sku' => (string) $matchedProduct->sku,
                    'unit' => $matchedProduct->unit ?? 'piece',
                    'unit_price' => Money::fromMinor($unitPriceMinor),
                    'quantity' => $qty,
                    'discount_percent' => ($lineSubtotalMinor > 0) ? round(($lineDiscMinor / $lineSubtotalMinor) * 100, 2) : 0,
                    'subtotal' => Money::fromMinor($lineSubtotalMinor),
                    'discount_amount' => Money::fromMinor($lineDiscMinor),
                    'tax_amount' => Money::fromMinor($lineTaxMinor),
                    'total_price' => Money::fromMinor($lineTotalMinor),
                    'created_at' => $parsedDate . ' 12:00:00',
                    'updated_at' => $parsedDate . ' 12:00:00',
                ];
            }

            // Total Amount calculation
            $totalAmountMinor = $subtotalMinor - $discountMinor + $taxMinor;
            if ($firstRow['total_raw'] !== null && trim((string) $firstRow['total_raw']) !== '') {
                $parsedTotal = DataNormalizer::parseMoney($firstRow['total_raw']);
                if ($parsedTotal !== null) {
                    $fileTotalMinor = Money::toMinor($parsedTotal);
                    if ($fileTotalMinor > 0) {
                        $totalAmountMinor = $fileTotalMinor;
                    }
                }
            }

            // Generate order_number
            $orderNumber = "HIST-{$externalOrderId}";
            if (Order::where('order_number', $orderNumber)->exists()) {
                $orderNumber = "HIST-{$externalOrderId}-" . substr(md5($orderDeterministicId), 0, 6);
            }

            // Persist Order: HISTORICAL_RECORD_ONLY
            $order = new Order();
            $order->timestamps = false;
            $order->fill([
                'id' => $orderDeterministicId,
                'order_number' => $orderNumber,
                'external_order_id' => $externalOrderId,
                'organization_id' => $job->organization_id,
                'store_id' => $resolvedStore->id,
                'warehouse_id' => $resolvedWarehouse ? $resolvedWarehouse->id : 'default',
                'order_date' => $parsedDate . ' 12:00:00',
                'customer_id' => $customerId,
                'customer_name' => $customerName,
                'subtotal' => Money::fromMinor($subtotalMinor),
                'discount_amount' => Money::fromMinor($discountMinor),
                'tax_amount' => Money::fromMinor($taxMinor),
                'tax_rate' => ($subtotalMinor > 0) ? round(($taxMinor / $subtotalMinor) * 100, 2) : 0,
                'total_amount' => Money::fromMinor($totalAmountMinor),
                'payment_method' => $paymentMethod,
                'payment_status' => $paymentStatus,
                'fulfillment_status' => $fulfillmentStatus,
                'notes' => $notes,
                'source' => 'IMPORT_HISTORICAL',
                'is_historical' => true,
                'import_job_id' => $job->id,
                'created_by_user_id' => $user->id,
            ]);
            $order->created_at = $parsedDate . ' 12:00:00';
            $order->updated_at = $parsedDate . ' 12:00:00';
            $order->save();

            // Insert line items (NO stock movements, NO accounting entries)
            foreach ($preparedLines as $lineData) {
                $item = new OrderItem();
                $item->timestamps = false;
                $item->fill($lineData);
                $item->created_at = $lineData['created_at'];
                $item->updated_at = $lineData['updated_at'];
                $item->save();
            }

            $importedRows += count($orderRows);
        });
    }

    /**
     * Execute suppliers import.
     */
    private function executeSuppliers(
        ImportJob $job,
        ImportSource $source,
        string $filePath,
        int $sheetIndex,
        array $mapping,
        string $duplicateStrategy
    ): array {
        $importedRows = 0;
        $skippedRows = 0;
        $failedRows = 0;

        $seenCodesInFile = [];
        $seenPhonesInFile = [];
        $seenEmailsInFile = [];

        $batch = [];
        $batchSize = 100;

        $rowGenerator = $this->readerService->streamRows($filePath, $source->file_format, $sheetIndex);

        foreach ($rowGenerator as $rowItem) {
            $batch[] = $rowItem;
            if (count($batch) >= $batchSize) {
                $this->processSuppliersBatch(
                    $batch,
                    $job,
                    $mapping,
                    $duplicateStrategy,
                    $seenCodesInFile,
                    $seenPhonesInFile,
                    $seenEmailsInFile,
                    $importedRows,
                    $skippedRows,
                    $failedRows
                );
                $batch = [];
            }
        }

        if (!empty($batch)) {
            $this->processSuppliersBatch(
                $batch,
                $job,
                $mapping,
                $duplicateStrategy,
                $seenCodesInFile,
                $seenPhonesInFile,
                $seenEmailsInFile,
                $importedRows,
                $skippedRows,
                $failedRows
            );
        }

        $job->update([
            'imported_rows' => $importedRows,
            'skipped_rows' => $skippedRows,
            'failed_rows' => $failedRows,
        ]);

        return [
            'imported_rows' => $importedRows,
            'skipped_rows' => $skippedRows,
            'failed_rows' => $failedRows,
        ];
    }

    private function processSuppliersBatch(
        array $batch,
        ImportJob $job,
        array $mapping,
        string $duplicateStrategy,
        array &$seenCodesInFile,
        array &$seenPhonesInFile,
        array &$seenEmailsInFile,
        int &$importedRows,
        int &$skippedRows,
        int &$failedRows
    ): void {
        DB::transaction(function () use (
            $batch,
            $job,
            $mapping,
            $duplicateStrategy,
            &$seenCodesInFile,
            &$seenPhonesInFile,
            &$seenEmailsInFile,
            &$importedRows,
            &$skippedRows,
            &$failedRows
        ) {
            foreach ($batch as $rowItem) {
                $rowNumber = $rowItem['row_number'];

                $nameRaw = $this->getMappedValue($rowItem, $mapping, 'name');
                $codeRaw = $this->getMappedValue($rowItem, $mapping, 'code');
                $phoneRaw = $this->getMappedValue($rowItem, $mapping, 'phone');
                $emailRaw = $this->getMappedValue($rowItem, $mapping, 'email');
                $addressRaw = $this->getMappedValue($rowItem, $mapping, 'address');
                $taxIdRaw = $this->getMappedValue($rowItem, $mapping, 'tax_id');
                $notesRaw = $this->getMappedValue($rowItem, $mapping, 'notes');

                $name = DataNormalizer::cleanString($nameRaw ?? '');
                $code = DataNormalizer::cleanString($codeRaw ?? '');
                $phone = DataNormalizer::normalizePhone($phoneRaw);
                $email = DataNormalizer::normalizeEmail($emailRaw);
                $address = DataNormalizer::cleanString($addressRaw ?? '');
                $taxId = DataNormalizer::cleanString($taxIdRaw ?? '');
                $notes = DataNormalizer::cleanString($notesRaw ?? '');

                if ($name === '' || mb_strlen($name, 'UTF-8') > 191) {
                    $failedRows++;
                    continue;
                }

                if ($code !== '' && mb_strlen($code, 'UTF-8') > 100) {
                    $failedRows++;
                    continue;
                }

                // In-file duplicate checking
                if ($code !== '' && isset($seenCodesInFile[$code])) {
                    $skippedRows++;
                    continue;
                }
                if ($phone !== null && isset($seenPhonesInFile[$phone])) {
                    $skippedRows++;
                    continue;
                }
                if ($email !== null && isset($seenEmailsInFile[$email])) {
                    $skippedRows++;
                    continue;
                }

                if ($code !== '') {
                    $seenCodesInFile[$code] = $rowNumber;
                }
                if ($phone !== null) {
                    $seenPhonesInFile[$phone] = $rowNumber;
                }
                if ($email !== null) {
                    $seenEmailsInFile[$email] = $rowNumber;
                }

                // Deterministic UUID for idempotency
                $deterministicId = ExpenseService::deterministicUuid("resto_imp_{$job->organization_id}_{$job->id}_sup_{$rowNumber}");

                // Check if already committed by this job
                $alreadyCommitted = Supplier::where('organization_id', $job->organization_id)
                    ->where('id', $deterministicId)
                    ->first();

                if ($alreadyCommitted) {
                    $skippedRows++;
                    continue;
                }

                // Check existing supplier in database: match only by code, phone, or email
                $existingSupplier = null;
                if ($code !== '') {
                    $existingSupplier = Supplier::where('organization_id', $job->organization_id)
                        ->where('code', $code)
                        ->first();
                }
                if (!$existingSupplier && $phone !== null) {
                    $existingSupplier = Supplier::where('organization_id', $job->organization_id)
                        ->where('phone', $phone)
                        ->first();
                }
                if (!$existingSupplier && $email !== null) {
                    $existingSupplier = Supplier::where('organization_id', $job->organization_id)
                        ->where('email', $email)
                        ->first();
                }

                if ($existingSupplier) {
                    if ($duplicateStrategy === self::STRATEGY_SKIP) {
                        $skippedRows++;
                        continue;
                    }

                    // UPDATE strategy: update permitted fields only
                    $updatePayload = [];
                    if ($name !== '') {
                        $updatePayload['name'] = $name;
                    }
                    if ($code !== '') {
                        $updatePayload['code'] = $code;
                    }
                    if ($phone !== null) {
                        $updatePayload['phone'] = $phone;
                    }
                    if ($email !== null) {
                        $updatePayload['email'] = $email;
                    }
                    if ($address !== '') {
                        $updatePayload['address'] = $address;
                    }
                    if ($taxId !== '') {
                        $updatePayload['tax_id'] = $taxId;
                    }
                    if ($notes !== '') {
                        $updatePayload['notes'] = $notes;
                    }

                    if (!empty($updatePayload)) {
                        $existingSupplier->update($updatePayload);
                    }
                    $importedRows++;
                    continue;
                }

                // Create fresh supplier
                $supplier = new Supplier();
                $supplier->fill([
                    'id' => $deterministicId,
                    'organization_id' => $job->organization_id,
                    'name' => $name,
                    'code' => $code ?: null,
                    'phone' => $phone ?: null,
                    'email' => $email ?: null,
                    'address' => $address ?: null,
                    'tax_id' => $taxId ?: null,
                    'notes' => $notes ?: null,
                ]);
                $supplier->save();
                $importedRows++;
            }
        });
    }

    /**
     * Execute historical purchases import adhering strictly to HISTORICAL_RECORD_ONLY domain contract.
     * ZERO mutations to WarehouseStock, ZERO stock movements, ZERO live general ledger journal entries.
     */
    private function executePurchases(
        ImportJob $job,
        ImportSource $source,
        string $filePath,
        int $sheetIndex,
        array $mapping,
        string $duplicateStrategy,
        User $user
    ): array {
        $importedRows = 0;
        $skippedRows = 0;
        $failedRows = 0;

        $defaultStoreId = $job->mapping_config['store_id'] ?? null;
        $tenantStores = Store::where('organization_id', $job->organization_id)->get();
        $defaultStore = $defaultStoreId ? $tenantStores->firstWhere('id', $defaultStoreId) : $tenantStores->first();

        $defaultWarehouseId = $job->mapping_config['warehouse_id'] ?? null;
        $tenantWarehouses = Warehouse::where('organization_id', $job->organization_id)->get();
        $defaultWarehouse = $defaultWarehouseId ? $tenantWarehouses->firstWhere('id', $defaultWarehouseId) : $tenantWarehouses->first();

        $defaultSupplierId = $job->mapping_config['supplier_id'] ?? null;
        $tenantSuppliers = Supplier::where('organization_id', $job->organization_id)->get();
        $defaultSupplier = $defaultSupplierId ? $tenantSuppliers->firstWhere('id', $defaultSupplierId) : $tenantSuppliers->first();

        $tenantProducts = Product::where('organization_id', $job->organization_id)->get();

        // 1. Group rows by purchase_id
        $groupedPurchases = [];
        $rowGenerator = $this->readerService->streamRows($filePath, $source->file_format, $sheetIndex);

        foreach ($rowGenerator as $rowItem) {
            $rowNumber = $rowItem['row_number'];
            $purchaseIdRaw = $this->getMappedValue($rowItem, $mapping, 'purchase_id');
            $skuRaw = $this->getMappedValue($rowItem, $mapping, 'sku');
            $qtyRaw = $this->getMappedValue($rowItem, $mapping, 'quantity');
            $costRaw = $this->getMappedValue($rowItem, $mapping, 'unit_cost');
            $dateRaw = $this->getMappedValue($rowItem, $mapping, 'date');
            $discRaw = $this->getMappedValue($rowItem, $mapping, 'discount_amount');
            $taxRaw = $this->getMappedValue($rowItem, $mapping, 'tax_amount');
            $totalRaw = $this->getMappedValue($rowItem, $mapping, 'total_amount');
            $supRaw = $this->getMappedValue($rowItem, $mapping, 'supplier');
            $storeRaw = $this->getMappedValue($rowItem, $mapping, 'store');
            $whRaw = $this->getMappedValue($rowItem, $mapping, 'warehouse');
            $statusRaw = $this->getMappedValue($rowItem, $mapping, 'status');
            $payStatusRaw = $this->getMappedValue($rowItem, $mapping, 'payment_status');
            $notesRaw = $this->getMappedValue($rowItem, $mapping, 'notes');

            $purchaseId = DataNormalizer::cleanString($purchaseIdRaw ?? '');
            $groupKey = ($purchaseId !== '') ? $purchaseId : ("__MISSING_PO_ROW_{$rowNumber}");

            $groupedPurchases[$groupKey][] = [
                'row_number' => $rowNumber,
                'purchase_id' => $purchaseId,
                'sku_raw' => $skuRaw,
                'qty_raw' => $qtyRaw,
                'cost_raw' => $costRaw,
                'date_raw' => $dateRaw,
                'disc_raw' => $discRaw,
                'tax_raw' => $taxRaw,
                'total_raw' => $totalRaw,
                'sup_raw' => $supRaw,
                'store_raw' => $storeRaw,
                'wh_raw' => $whRaw,
                'status_raw' => $statusRaw,
                'pay_status_raw' => $payStatusRaw,
                'notes_raw' => $notesRaw,
            ];
        }

        // 2. Process each purchase group inside its own database transaction
        foreach ($groupedPurchases as $groupKey => $purchaseRows) {
            $this->processPurchaseGroup(
                $purchaseRows,
                $job,
                $duplicateStrategy,
                $tenantStores,
                $defaultStore,
                $tenantWarehouses,
                $defaultWarehouse,
                $tenantSuppliers,
                $defaultSupplier,
                $tenantProducts,
                $user,
                $importedRows,
                $skippedRows,
                $failedRows
            );
        }

        $job->update([
            'imported_rows' => $importedRows,
            'skipped_rows' => $skippedRows,
            'failed_rows' => $failedRows,
        ]);

        return [
            'imported_rows' => $importedRows,
            'skipped_rows' => $skippedRows,
            'failed_rows' => $failedRows,
        ];
    }

    private function processPurchaseGroup(
        array $purchaseRows,
        ImportJob $job,
        string $duplicateStrategy,
        $tenantStores,
        ?Store $defaultStore,
        $tenantWarehouses,
        ?Warehouse $defaultWarehouse,
        $tenantSuppliers,
        ?Supplier $defaultSupplier,
        $tenantProducts,
        User $user,
        int &$importedRows,
        int &$skippedRows,
        int &$failedRows
    ): void {
        DB::transaction(function () use (
            $purchaseRows,
            $job,
            $duplicateStrategy,
            $tenantStores,
            $defaultStore,
            $tenantWarehouses,
            $defaultWarehouse,
            $tenantSuppliers,
            $defaultSupplier,
            $tenantProducts,
            $user,
            &$importedRows,
            &$skippedRows,
            &$failedRows
        ) {
            $firstRow = $purchaseRows[0];
            $externalPurchaseId = $firstRow['purchase_id'];

            if ($externalPurchaseId === '') {
                $failedRows += count($purchaseRows);
                return;
            }

            // Parse Date
            $parsedDate = DataNormalizer::parseDate($firstRow['date_raw']);
            if (!$parsedDate) {
                $failedRows += count($purchaseRows);
                return;
            }

            // Resolve Store
            $storeRaw = DataNormalizer::cleanString($firstRow['store_raw'] ?? '');
            $resolvedStore = null;
            if ($storeRaw !== '') {
                $resolvedStore = $tenantStores->first(function ($st) use ($storeRaw) {
                    return mb_strtolower($st->code ?? '', 'UTF-8') === mb_strtolower($storeRaw, 'UTF-8')
                        || mb_strtolower($st->name ?? '', 'UTF-8') === mb_strtolower($storeRaw, 'UTF-8')
                        || $st->id === $storeRaw;
                });
            }
            if (!$resolvedStore) {
                $resolvedStore = $defaultStore;
            }
            if (!$resolvedStore) {
                $failedRows += count($purchaseRows);
                return;
            }

            // Resolve Warehouse
            $whRaw = DataNormalizer::cleanString($firstRow['wh_raw'] ?? '');
            $resolvedWarehouse = null;
            if ($whRaw !== '') {
                $resolvedWarehouse = $tenantWarehouses->first(function ($wh) use ($whRaw) {
                    return mb_strtolower($wh->code ?? '', 'UTF-8') === mb_strtolower($whRaw, 'UTF-8')
                        || mb_strtolower($wh->name ?? '', 'UTF-8') === mb_strtolower($whRaw, 'UTF-8')
                        || $wh->id === $whRaw;
                });
            }
            if (!$resolvedWarehouse) {
                $resolvedWarehouse = $defaultWarehouse;
            }
            if (!$resolvedWarehouse) {
                $failedRows += count($purchaseRows);
                return;
            }

            // Resolve Supplier
            $supRaw = DataNormalizer::cleanString($firstRow['sup_raw'] ?? '');
            $resolvedSupplier = null;
            if ($supRaw !== '') {
                $resolvedSupplier = $tenantSuppliers->first(function ($s) use ($supRaw) {
                    return $s->id === $supRaw
                        || ($s->code && mb_strtolower($s->code, 'UTF-8') === mb_strtolower($supRaw, 'UTF-8'))
                        || ($s->phone && $s->phone === DataNormalizer::normalizePhone($supRaw))
                        || ($s->email && $s->email === DataNormalizer::normalizeEmail($supRaw))
                        || mb_strtolower($s->name, 'UTF-8') === mb_strtolower($supRaw, 'UTF-8');
                });
            }
            if (!$resolvedSupplier) {
                $resolvedSupplier = $defaultSupplier;
            }
            if (!$resolvedSupplier) {
                $failedRows += count($purchaseRows);
                return;
            }

            // Status and Payment Status
            $statusRaw = strtoupper(DataNormalizer::cleanString($firstRow['status_raw'] ?? '') ?: 'RECEIVED');
            $status = in_array($statusRaw, ['RECEIVED', 'ORDERED', 'PARTIALLY_RECEIVED', 'CANCELLED'], true) ? $statusRaw : 'RECEIVED';
            $paymentStatus = strtoupper(DataNormalizer::cleanString($firstRow['pay_status_raw'] ?? '') ?: 'PAID');
            $notes = DataNormalizer::cleanString($firstRow['notes_raw'] ?? '');

            // Deterministic Purchase UUID
            $purchaseDeterministicId = ExpenseService::deterministicUuid("resto_imp_{$job->organization_id}_{$job->id}_po_{$externalPurchaseId}");

            // Check if this purchase was already committed by this job (retry idempotency)
            $alreadyCommittedByJob = Purchase::where('organization_id', $job->organization_id)
                ->where('id', $purchaseDeterministicId)
                ->first();

            if ($alreadyCommittedByJob) {
                $skippedRows += count($purchaseRows);
                return;
            }

            // Check for existing matching Purchase in database (by external_purchase_id or purchase_number)
            $existingPurchase = Purchase::where('organization_id', $job->organization_id)
                ->where(function ($q) use ($externalPurchaseId) {
                    $q->where('external_purchase_id', $externalPurchaseId)
                      ->orWhere('purchase_number', $externalPurchaseId);
                })
                ->first();

            if ($existingPurchase) {
                if ($duplicateStrategy === self::STRATEGY_SKIP) {
                    $skippedRows += count($purchaseRows);
                    return;
                }

                // UPDATE Strategy: only update descriptive non-financial attributes on historical purchase
                $updatePayload = [];
                if ($notes !== '') {
                    $updatePayload['notes'] = $notes;
                }
                if (!empty($updatePayload)) {
                    $existingPurchase->update($updatePayload);
                }
                $importedRows += count($purchaseRows);
                return;
            }

            // Validate and prepare line items
            $preparedLines = [];
            $subtotalMinor = 0;
            $discountMinor = 0;
            $taxMinor = 0;

            foreach ($purchaseRows as $rowItem) {
                $skuNorm = DataNormalizer::cleanString(DataNormalizer::toEnglishDigits($rowItem['sku_raw'] ?? ''));
                $matchedProduct = $tenantProducts->first(function ($p) use ($skuNorm) {
                    return (string) $p->sku === $skuNorm
                        || (string) $p->barcode === $skuNorm
                        || (string) $p->id === $skuNorm;
                });

                if (!$matchedProduct) {
                    $failedRows += count($purchaseRows);
                    return;
                }

                $qtyNorm = DataNormalizer::cleanString(DataNormalizer::toEnglishDigits($rowItem['qty_raw'] ?? ''));
                if ($qtyNorm === '' || !ctype_digit($qtyNorm) || (int) $qtyNorm <= 0) {
                    $failedRows += count($purchaseRows);
                    return;
                }
                $qty = (int) $qtyNorm;

                $costNorm = DataNormalizer::parseMoney($rowItem['cost_raw']);
                if ($costNorm === null || (float) $costNorm < 0) {
                    $failedRows += count($purchaseRows);
                    return;
                }

                $unitCostMinor = Money::toMinor($costNorm);
                $lineSubtotalMinor = Money::multiply($unitCostMinor, $qty);

                $lineDiscMinor = 0;
                if ($rowItem['disc_raw'] !== null && trim((string) $rowItem['disc_raw']) !== '') {
                    $discNorm = DataNormalizer::parseMoney($rowItem['disc_raw']);
                    if ($discNorm !== null) {
                        $lineDiscMinor = Money::toMinor($discNorm);
                    }
                }

                $lineTaxMinor = 0;
                if ($rowItem['tax_raw'] !== null && trim((string) $rowItem['tax_raw']) !== '') {
                    $taxNorm = DataNormalizer::parseMoney($rowItem['tax_raw']);
                    if ($taxNorm !== null) {
                        $lineTaxMinor = Money::toMinor($taxNorm);
                    }
                }

                $lineTotalMinor = $lineSubtotalMinor - $lineDiscMinor + $lineTaxMinor;

                $subtotalMinor += $lineSubtotalMinor;
                $discountMinor += $lineDiscMinor;
                $taxMinor += $lineTaxMinor;

                $lineDeterministicId = ExpenseService::deterministicUuid("resto_imp_{$job->organization_id}_{$job->id}_poitem_{$externalPurchaseId}_{$rowItem['row_number']}");

                $preparedLines[] = [
                    'id' => $lineDeterministicId,
                    'purchase_id' => $purchaseDeterministicId,
                    'product_id' => $matchedProduct->id,
                    'unit' => $matchedProduct->unit ?? 'piece',
                    'quantity' => $qty,
                    'received_quantity' => ($status === 'RECEIVED') ? $qty : 0,
                    'unit_cost' => Money::fromMinor($unitCostMinor),
                    'total_cost' => Money::fromMinor($lineTotalMinor),
                    'subtotal' => Money::fromMinor($lineSubtotalMinor),
                    'discount_amount' => Money::fromMinor($lineDiscMinor),
                    'tax_amount' => Money::fromMinor($lineTaxMinor),
                    'created_at' => $parsedDate . ' 12:00:00',
                    'updated_at' => $parsedDate . ' 12:00:00',
                ];
            }

            // Total Amount calculation
            $totalAmountMinor = $subtotalMinor - $discountMinor + $taxMinor;
            if ($firstRow['total_raw'] !== null && trim((string) $firstRow['total_raw']) !== '') {
                $parsedTotal = DataNormalizer::parseMoney($firstRow['total_raw']);
                if ($parsedTotal !== null) {
                    $fileTotalMinor = Money::toMinor($parsedTotal);
                    if ($fileTotalMinor > 0) {
                        $totalAmountMinor = $fileTotalMinor;
                    }
                }
            }

            // Generate purchase_number
            $purchaseNumber = "HIST-PO-{$externalPurchaseId}";
            if (Purchase::where('purchase_number', $purchaseNumber)->exists()) {
                $purchaseNumber = "HIST-PO-{$externalPurchaseId}-" . substr(md5($purchaseDeterministicId), 0, 6);
            }

            // Persist Purchase: HISTORICAL_RECORD_ONLY (ZERO inventory, ZERO GL, ZERO supplier payments)
            $purchase = new Purchase();
            $purchase->timestamps = false;
            $purchase->fill([
                'id' => $purchaseDeterministicId,
                'organization_id' => $job->organization_id,
                'store_id' => $resolvedStore->id,
                'warehouse_id' => $resolvedWarehouse->id,
                'supplier_id' => $resolvedSupplier->id,
                'purchase_number' => $purchaseNumber,
                'external_purchase_id' => $externalPurchaseId,
                'purchase_date' => $parsedDate,
                'subtotal' => Money::fromMinor($subtotalMinor),
                'discount_amount' => Money::fromMinor($discountMinor),
                'tax_amount' => Money::fromMinor($taxMinor),
                'total_amount' => Money::fromMinor($totalAmountMinor),
                'status' => $status,
                'payment_status' => $paymentStatus,
                'notes' => $notes,
                'source' => 'IMPORT_HISTORICAL',
                'is_historical' => true,
                'import_job_id' => $job->id,
                'received_at' => ($status === 'RECEIVED') ? ($parsedDate . ' 12:00:00') : null,
                'last_received_at' => ($status === 'RECEIVED') ? ($parsedDate . ' 12:00:00') : null,
            ]);
            $purchase->created_at = $parsedDate . ' 12:00:00';
            $purchase->updated_at = $parsedDate . ' 12:00:00';
            $purchase->save();

            // Insert line items
            foreach ($preparedLines as $lineData) {
                $item = new PurchaseItem();
                $item->timestamps = false;
                $item->fill($lineData);
                $item->created_at = $lineData['created_at'];
                $item->updated_at = $lineData['updated_at'];
                $item->save();
            }

            $importedRows += count($purchaseRows);
        });
    }

    private function getMappedValue(array $rowItem, array $mapping, string $targetField): ?string
    {
        $sourceCol = $mapping[$targetField] ?? null;
        if ($sourceCol === null || $sourceCol === '') {
            return null;
        }

        if (isset($rowItem['assoc'][$sourceCol])) {
            return $rowItem['assoc'][$sourceCol];
        }

        if (is_numeric($sourceCol) && isset($rowItem['row'][(int) $sourceCol])) {
            return $rowItem['row'][(int) $sourceCol];
        }

        return null;
    }
}
