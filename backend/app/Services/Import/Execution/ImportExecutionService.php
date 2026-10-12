<?php

namespace App\Services\Import\Execution;

use App\Models\Customer;
use App\Models\Expense;
use App\Models\ImportJob;
use App\Models\ImportSource;
use App\Models\Product;
use App\Models\Store;
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
