<?php

namespace App\Services\Import\Validation;

use App\Models\Customer;
use App\Models\Expense;
use App\Models\ImportDataset;
use App\Models\ImportJob;
use App\Models\ImportSource;
use App\Models\ImportValidationResult;
use App\Models\Order;
use App\Models\OrderItem;
use App\Models\Product;
use App\Models\Store;
use App\Models\Warehouse;
use App\Models\WarehouseStock;
use App\Services\Import\ImportStateMachine;
use App\Services\Import\Mapping\ImportMappingService;
use App\Services\Import\Normalizer\DataNormalizer;
use App\Services\Import\Reader\SpreadsheetReaderService;
use App\Support\Import\ImportErrorCode;
use App\Support\Import\ImportStatus;
use App\Support\Import\ImportType;
use App\Support\Money;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Facades\Storage;
use Illuminate\Support\Str;
use InvalidArgumentException;
use RuntimeException;

class ImportValidationService
{
    public const PREVIEW_LIMIT = 50;

    public function __construct(
        private ImportMappingService $mappingService,
        private SpreadsheetReaderService $readerService,
        private ImportStateMachine $stateMachine,
    ) {
    }

    /**
     * Validate an import job against its stored mapping configuration.
     * Generates ImportValidationResult rows, collects preview rows, and updates counters.
     *
     * @return array<string, mixed> Summary of validation
     */
    public function validateJob(ImportJob $job, ?int $sheetIndex = null): array
    {
        if (!$job->mapping_config || empty($job->mapping_config['fields'])) {
            throw new InvalidArgumentException('پیکربندی تطبیق ستون‌ها برای این عملیات ثبت نشده است.');
        }

        // Must be in ready_to_validate or mapping_required
        if ($job->status === ImportStatus::MAPPING_REQUIRED) {
            $this->stateMachine->transition($job, ImportStatus::READY_TO_VALIDATE, 'Ready for validation');
        }

        if ($job->status !== ImportStatus::READY_TO_VALIDATE) {
            throw new InvalidArgumentException("عملیات در وضعیت '{$job->status}' امکان اعتبارسنجی ندارد.");
        }

        // Transition to validating
        $this->stateMachine->transition($job, ImportStatus::VALIDATING, 'Validation started');

        try {
            $source = $job->sources()->first();
            if (!$source) {
                throw new RuntimeException('فایل منبع یافت نشد.');
            }

            $storageDisk = Storage::disk('local');
            $absolutePath = $storageDisk->path($source->storage_path);

            $targetSheetIndex = $sheetIndex ?? (int) ($job->mapping_config['sheet_index'] ?? 0);
            $mapping = $job->mapping_config['fields'] ?? [];

            // Clear any previous validation results for this job
            ImportValidationResult::where('import_job_id', $job->id)->delete();

            $validationReport = match ($job->import_type) {
                ImportType::PRODUCTS => $this->validateProducts($job, $source, $absolutePath, $targetSheetIndex, $mapping),
                ImportType::CUSTOMERS => $this->validateCustomers($job, $source, $absolutePath, $targetSheetIndex, $mapping),
                ImportType::INVENTORY => $this->validateInventory($job, $source, $absolutePath, $targetSheetIndex, $mapping),
                ImportType::EXPENSES => $this->validateExpenses($job, $source, $absolutePath, $targetSheetIndex, $mapping),
                ImportType::ORDERS => $this->validateOrders($job, $source, $absolutePath, $targetSheetIndex, $mapping),
                default => throw new InvalidArgumentException("نوع درون‌ریزی '{$job->import_type}' پشتیبانی نمی‌شود."),
            };

            // Transition to preview_ready
            $this->stateMachine->transition(
                $job,
                ImportStatus::PREVIEW_READY,
                'Validation completed and preview generated.'
            );

            return $validationReport;

        } catch (\Throwable $e) {
            Log::error("Validation failed for job {$job->id}: {$e->getMessage()}");
            $this->stateMachine->transition($job, ImportStatus::FAILED, 'خطا در اعتبارسنجی: ' . $e->getMessage());
            throw $e;
        }
    }

    /**
     * Validate product rows.
     */
    private function validateProducts(
        ImportJob $job,
        ImportSource $source,
        string $filePath,
        int $sheetIndex,
        array $mapping
    ): array {
        $seenSkusInFile = [];
        $seenBarcodesInFile = [];

        $totalRows = 0;
        $validRows = 0;
        $invalidRows = 0;
        $matchedExistingRows = 0;

        $previewRows = [];
        $validationResultsToInsert = [];

        $rowGenerator = $this->readerService->streamRows($filePath, $source->file_format, $sheetIndex);

        foreach ($rowGenerator as $rowItem) {
            $rowNumber = $rowItem['row_number'];
            $totalRows++;

            $rowErrors = [];
            $rowWarnings = [];

            // Extract mapped fields
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

            // 1. Required: Name
            if ($name === '') {
                $rowErrors[] = [
                    'field' => 'name',
                    'code' => ImportErrorCode::REQUIRED_FIELD_MISSING,
                    'message' => ImportErrorCode::persianMessage(ImportErrorCode::REQUIRED_FIELD_MISSING, ['field' => 'نام کالا']),
                    'value' => (string) $nameRaw,
                ];
            } elseif (mb_strlen($name) > 191) {
                $rowErrors[] = [
                    'field' => 'name',
                    'code' => ImportErrorCode::STRING_TOO_LONG,
                    'message' => 'طول نام کالا نباید بیش از ۱۹۱ کاراکتر باشد.',
                    'value' => $name,
                ];
            }

            // 2. Required: SKU
            if ($sku === '') {
                $rowErrors[] = [
                    'field' => 'sku',
                    'code' => ImportErrorCode::REQUIRED_FIELD_MISSING,
                    'message' => ImportErrorCode::persianMessage(ImportErrorCode::REQUIRED_FIELD_MISSING, ['field' => 'کد کالا / SKU']),
                    'value' => (string) $skuRaw,
                ];
            } elseif (mb_strlen($sku) > 100) {
                $rowErrors[] = [
                    'field' => 'sku',
                    'code' => ImportErrorCode::STRING_TOO_LONG,
                    'message' => 'طول کد کالا (SKU) نباید بیش از ۱۰۰ کاراکتر باشد.',
                    'value' => $sku,
                ];
            } else {
                // Check in-file duplicate SKU
                $skuLower = mb_strtolower($sku);
                if (isset($seenSkusInFile[$skuLower])) {
                    $rowErrors[] = [
                        'field' => 'sku',
                        'code' => ImportErrorCode::DUPLICATE_IN_FILE,
                        'message' => "کد کالا '{$sku}' در ردیف {$seenSkusInFile[$skuLower]} نیز تکرار شده است.",
                        'value' => $sku,
                    ];
                } else {
                    $seenSkusInFile[$skuLower] = $rowNumber;
                }
            }

            // 3. Required: Price
            $priceParsed = DataNormalizer::parseMoney($priceRaw);
            if ($priceParsed === null) {
                $rowErrors[] = [
                    'field' => 'price',
                    'code' => ImportErrorCode::NUMERIC_INVALID,
                    'message' => 'قیمت فروش نامعتبر است یا خالی وارد شده است.',
                    'value' => (string) $priceRaw,
                ];
            }

            // 4. Optional: Cost Price
            $costPriceParsed = '0.00';
            if ($costPriceRaw !== null && trim($costPriceRaw) !== '') {
                $costPriceParsed = DataNormalizer::parseMoney($costPriceRaw);
                if ($costPriceParsed === null) {
                    $rowErrors[] = [
                        'field' => 'cost_price',
                        'code' => ImportErrorCode::NUMERIC_INVALID,
                        'message' => 'قیمت خرید نامعتبر است.',
                        'value' => (string) $costPriceRaw,
                    ];
                }
            }

            // 5. Optional: Unit
            $normalizedUnit = DataNormalizer::normalizeUnit($unitRaw);
            if ($normalizedUnit === null) {
                $rowErrors[] = [
                    'field' => 'unit',
                    'code' => ImportErrorCode::INVALID_UNIT,
                    'message' => "واحد سنجش '{$unitRaw}' شناخته شده نیست.",
                    'value' => (string) $unitRaw,
                ];
            }

            // 6. Optional: Barcode
            if ($barcode !== '') {
                if (mb_strlen($barcode) > 100) {
                    $rowErrors[] = [
                        'field' => 'barcode',
                        'code' => ImportErrorCode::STRING_TOO_LONG,
                        'message' => 'طول بارکد نباید بیش از ۱۰۰ کاراکتر باشد.',
                        'value' => $barcode,
                    ];
                } else {
                    $barcodeLower = mb_strtolower($barcode);
                    if (isset($seenBarcodesInFile[$barcodeLower])) {
                        $rowErrors[] = [
                            'field' => 'barcode',
                            'code' => ImportErrorCode::DUPLICATE_IN_FILE,
                            'message' => "بارکد '{$barcode}' در ردیف {$seenBarcodesInFile[$barcodeLower]} نیز تکرار شده است.",
                            'value' => $barcode,
                        ];
                    } else {
                        $seenBarcodesInFile[$barcodeLower] = $rowNumber;
                    }
                }
            }

            // 7. Check match against existing database record (Organization-scoped)
            $existingProduct = null;
            if ($sku !== '') {
                $existingProduct = Product::where('organization_id', $job->organization_id)
                    ->where('sku', $sku)
                    ->first();
            }

            if (!$existingProduct && $barcode !== '') {
                $existingProduct = Product::where('organization_id', $job->organization_id)
                    ->where('barcode', $barcode)
                    ->first();
            }

            $matchedExisting = false;
            if ($existingProduct) {
                $matchedExisting = true;
                $matchedExistingRows++;
                $rowWarnings[] = [
                    'field' => 'sku',
                    'code' => ImportErrorCode::EXISTING_RECORD_MATCH,
                    'message' => "کالا با نام '{$existingProduct->name}' و کد مشابه در سیستم موجود است.",
                    'value' => $sku ?: $barcode,
                ];
            }

            $isRowValid = empty($rowErrors);
            if ($isRowValid) {
                $validRows++;
            } else {
                $invalidRows++;
            }

            // Record DB validation results
            foreach ($rowErrors as $err) {
                $validationResultsToInsert[] = [
                    'id' => (string) Str::uuid(),
                    'import_job_id' => $job->id,
                    'organization_id' => $job->organization_id,
                    'source_file_id' => $source->id,
                    'worksheet_name' => 'Sheet' . ($sheetIndex + 1),
                    'row_number' => $rowNumber,
                    'field_name' => $err['field'],
                    'original_value' => mb_substr($err['value'] ?? '', 0, 255),
                    'error_code' => $err['code'],
                    'message' => $err['message'],
                    'suggested_action' => 'مقدار این فیلد را در فایل منبع تصحیح نمایید.',
                    'severity' => 'error',
                    'is_eligible' => false,
                    'created_at' => now(),
                    'updated_at' => now(),
                ];
            }

            foreach ($rowWarnings as $warn) {
                $validationResultsToInsert[] = [
                    'id' => (string) Str::uuid(),
                    'import_job_id' => $job->id,
                    'organization_id' => $job->organization_id,
                    'source_file_id' => $source->id,
                    'worksheet_name' => 'Sheet' . ($sheetIndex + 1),
                    'row_number' => $rowNumber,
                    'field_name' => $warn['field'],
                    'original_value' => mb_substr($warn['value'] ?? '', 0, 255),
                    'error_code' => $warn['code'],
                    'message' => $warn['message'],
                    'suggested_action' => 'در مرحله تایید می‌توانید بازنویسی یا رد کردن را انتخاب کنید.',
                    'severity' => 'warning',
                    'is_eligible' => true,
                    'created_at' => now(),
                    'updated_at' => now(),
                ];
            }

            // Collect preview rows up to limit
            if (count($previewRows) < self::PREVIEW_LIMIT) {
                $previewRows[] = [
                    'row_number' => $rowNumber,
                    'is_valid' => $isRowValid,
                    'has_match' => $matchedExisting,
                    'errors' => $rowErrors,
                    'warnings' => $rowWarnings,
                    'mapped_data' => [
                        'name' => $name,
                        'sku' => $sku,
                        'price' => $priceParsed,
                        'cost_price' => $costPriceParsed,
                        'unit' => $normalizedUnit,
                        'barcode' => $barcode,
                        'category' => $category,
                        'description' => $description,
                    ],
                ];
            }
        }

        // Chunk insert validation results
        foreach (array_chunk($validationResultsToInsert, 200) as $chunk) {
            ImportValidationResult::insert($chunk);
        }

        // Update Job state and preview metadata
        $existingMeta = $job->metadata ?? [];
        $existingMeta['preview_rows'] = $previewRows;
        $existingMeta['matched_existing_rows'] = $matchedExistingRows;

        $job->update([
            'total_rows' => $totalRows,
            'processed_rows' => $totalRows,
            'valid_rows' => $validRows,
            'invalid_rows' => $invalidRows,
            'metadata' => $existingMeta,
        ]);

        return [
            'total_rows' => $totalRows,
            'valid_rows' => $validRows,
            'invalid_rows' => $invalidRows,
            'matched_existing_rows' => $matchedExistingRows,
            'preview_count' => count($previewRows),
        ];
    }

    /**
     * Validate customer rows.
     */
    private function validateCustomers(
        ImportJob $job,
        ImportSource $source,
        string $filePath,
        int $sheetIndex,
        array $mapping
    ): array {
        $seenPhonesInFile = [];
        $seenEmailsInFile = [];

        $totalRows = 0;
        $validRows = 0;
        $invalidRows = 0;
        $matchedExistingRows = 0;

        $previewRows = [];
        $validationResultsToInsert = [];

        $rowGenerator = $this->readerService->streamRows($filePath, $source->file_format, $sheetIndex);

        foreach ($rowGenerator as $rowItem) {
            $rowNumber = $rowItem['row_number'];
            $totalRows++;

            $rowErrors = [];
            $rowWarnings = [];

            // Extract mapped fields
            $nameRaw = $this->getMappedValue($rowItem, $mapping, 'name');
            $phoneRaw = $this->getMappedValue($rowItem, $mapping, 'phone');
            $emailRaw = $this->getMappedValue($rowItem, $mapping, 'email');
            $addressRaw = $this->getMappedValue($rowItem, $mapping, 'address');

            $name = DataNormalizer::cleanString($nameRaw);
            $phone = DataNormalizer::normalizePhone($phoneRaw);
            $email = DataNormalizer::normalizeEmail($emailRaw);
            $address = DataNormalizer::cleanString($addressRaw);

            // 1. Required: Name
            if ($name === '') {
                $rowErrors[] = [
                    'field' => 'name',
                    'code' => ImportErrorCode::REQUIRED_FIELD_MISSING,
                    'message' => ImportErrorCode::persianMessage(ImportErrorCode::REQUIRED_FIELD_MISSING, ['field' => 'نام مشتری']),
                    'value' => (string) $nameRaw,
                ];
            } elseif (mb_strlen($name) > 191) {
                $rowErrors[] = [
                    'field' => 'name',
                    'code' => ImportErrorCode::STRING_TOO_LONG,
                    'message' => 'طول نام مشتری نباید بیش از ۱۹۱ کاراکتر باشد.',
                    'value' => $name,
                ];
            }

            // 2. Optional: Phone
            if ($phoneRaw !== null && trim($phoneRaw) !== '') {
                if ($phone === null) {
                    $rowErrors[] = [
                        'field' => 'phone',
                        'code' => ImportErrorCode::INVALID_PHONE,
                        'message' => "شماره تماس '{$phoneRaw}' نامعتبر است.",
                        'value' => (string) $phoneRaw,
                    ];
                } else {
                    if (isset($seenPhonesInFile[$phone])) {
                        $rowErrors[] = [
                            'field' => 'phone',
                            'code' => ImportErrorCode::DUPLICATE_IN_FILE,
                            'message' => "شماره تماس '{$phone}' در ردیف {$seenPhonesInFile[$phone]} نیز تکرار شده است.",
                            'value' => $phone,
                        ];
                    } else {
                        $seenPhonesInFile[$phone] = $rowNumber;
                    }
                }
            }

            // 3. Optional: Email
            if ($emailRaw !== null && trim($emailRaw) !== '') {
                if ($email === null) {
                    $rowErrors[] = [
                        'field' => 'email',
                        'code' => ImportErrorCode::INVALID_EMAIL,
                        'message' => "آدرس ایمیل '{$emailRaw}' نامعتبر است.",
                        'value' => (string) $emailRaw,
                    ];
                } else {
                    if (isset($seenEmailsInFile[$email])) {
                        $rowErrors[] = [
                            'field' => 'email',
                            'code' => ImportErrorCode::DUPLICATE_IN_FILE,
                            'message' => "آدرس ایمیل '{$email}' در ردیف {$seenEmailsInFile[$email]} نیز تکرار شده است.",
                            'value' => $email,
                        ];
                    } else {
                        $seenEmailsInFile[$email] = $rowNumber;
                    }
                }
            }

            // 4. Check match against existing database customer (Organization-scoped)
            $existingCustomer = null;
            if ($phone !== null) {
                $existingCustomer = Customer::where('organization_id', $job->organization_id)
                    ->where('phone', $phone)
                    ->first();
            }

            if (!$existingCustomer && $email !== null) {
                $existingCustomer = Customer::where('organization_id', $job->organization_id)
                    ->where('email', $email)
                    ->first();
            }

            $matchedExisting = false;
            if ($existingCustomer) {
                $matchedExisting = true;
                $matchedExistingRows++;
                $rowWarnings[] = [
                    'field' => 'phone',
                    'code' => ImportErrorCode::EXISTING_RECORD_MATCH,
                    'message' => "مشتری با نام '{$existingCustomer->name}' و شماره/ایمیل مشابه قبلاً ثبت شده است.",
                    'value' => $phone ?: $email,
                ];
            }

            $isRowValid = empty($rowErrors);
            if ($isRowValid) {
                $validRows++;
            } else {
                $invalidRows++;
            }

            // Record DB validation results
            foreach ($rowErrors as $err) {
                $validationResultsToInsert[] = [
                    'id' => (string) Str::uuid(),
                    'import_job_id' => $job->id,
                    'organization_id' => $job->organization_id,
                    'source_file_id' => $source->id,
                    'worksheet_name' => 'Sheet' . ($sheetIndex + 1),
                    'row_number' => $rowNumber,
                    'field_name' => $err['field'],
                    'original_value' => mb_substr($err['value'] ?? '', 0, 255),
                    'error_code' => $err['code'],
                    'message' => $err['message'],
                    'suggested_action' => 'مقدار این فیلد را در فایل منبع تصحیح نمایید.',
                    'severity' => 'error',
                    'is_eligible' => false,
                    'created_at' => now(),
                    'updated_at' => now(),
                ];
            }

            foreach ($rowWarnings as $warn) {
                $validationResultsToInsert[] = [
                    'id' => (string) Str::uuid(),
                    'import_job_id' => $job->id,
                    'organization_id' => $job->organization_id,
                    'source_file_id' => $source->id,
                    'worksheet_name' => 'Sheet' . ($sheetIndex + 1),
                    'row_number' => $rowNumber,
                    'field_name' => $warn['field'],
                    'original_value' => mb_substr($warn['value'] ?? '', 0, 255),
                    'error_code' => $warn['code'],
                    'message' => $warn['message'],
                    'suggested_action' => 'در مرحله تایید می‌توانید بازنویسی یا رد کردن را انتخاب کنید.',
                    'severity' => 'warning',
                    'is_eligible' => true,
                    'created_at' => now(),
                    'updated_at' => now(),
                ];
            }

            // Collect preview rows up to limit
            if (count($previewRows) < self::PREVIEW_LIMIT) {
                $previewRows[] = [
                    'row_number' => $rowNumber,
                    'is_valid' => $isRowValid,
                    'has_match' => $matchedExisting,
                    'errors' => $rowErrors,
                    'warnings' => $rowWarnings,
                    'mapped_data' => [
                        'name' => $name,
                        'phone' => $phone,
                        'email' => $email,
                        'address' => $address,
                    ],
                ];
            }
        }

        // Chunk insert validation results
        foreach (array_chunk($validationResultsToInsert, 200) as $chunk) {
            ImportValidationResult::insert($chunk);
        }

        // Update Job state and preview metadata
        $existingMeta = $job->metadata ?? [];
        $existingMeta['preview_rows'] = $previewRows;
        $existingMeta['matched_existing_rows'] = $matchedExistingRows;

        $job->update([
            'total_rows' => $totalRows,
            'processed_rows' => $totalRows,
            'valid_rows' => $validRows,
            'invalid_rows' => $invalidRows,
            'metadata' => $existingMeta,
        ]);

        return [
            'total_rows' => $totalRows,
            'valid_rows' => $validRows,
            'invalid_rows' => $invalidRows,
            'matched_existing_rows' => $matchedExistingRows,
            'preview_count' => count($previewRows),
        ];
    }

    /**
     * Validate inventory opening balance rows.
     */
    private function validateInventory(
        ImportJob $job,
        ImportSource $source,
        string $filePath,
        int $sheetIndex,
        array $mapping
    ): array {
        $seenItemWarehouseInFile = [];

        $totalRows = 0;
        $validRows = 0;
        $invalidRows = 0;
        $matchedExistingRows = 0;

        $previewRows = [];
        $validationResultsToInsert = [];

        $defaultWarehouseId = $job->mapping_config['warehouse_id'] ?? null;
        $tenantWarehouses = Warehouse::where('organization_id', $job->organization_id)->get();
        $defaultWarehouse = $defaultWarehouseId ? $tenantWarehouses->firstWhere('id', $defaultWarehouseId) : null;

        $rowGenerator = $this->readerService->streamRows($filePath, $source->file_format, $sheetIndex);

        foreach ($rowGenerator as $rowItem) {
            $rowNumber = $rowItem['row_number'];
            $totalRows++;

            $rowErrors = [];
            $rowWarnings = [];

            // Extract mapped fields
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

            // 1. Identify Product (Tenant-isolated, pre-existing only)
            $matchedProduct = null;
            if ($sku === '' && $barcode === '') {
                $rowErrors[] = [
                    'field' => 'sku',
                    'code' => ImportErrorCode::REQUIRED_FIELD_MISSING,
                    'message' => 'کد یا شناسه کالا (SKU) الزامی است.',
                    'value' => (string) $skuRaw,
                ];
            } else {
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
                if ($candidates->count() > 1) {
                    $rowErrors[] = [
                        'field' => 'sku',
                        'code' => ImportErrorCode::AMBIGUOUS_PRODUCT,
                        'message' => "چندین کالا با شناسه مشابه یافت شد ({$candidates->count()} مورد).",
                        'value' => $sku ?: $barcode,
                    ];
                } elseif ($candidates->isEmpty()) {
                    $rowErrors[] = [
                        'field' => 'sku',
                        'code' => ImportErrorCode::PRODUCT_NOT_FOUND,
                        'message' => "کالایی با شناسه '{$sku}' در سامانه یافت نشد. موجودی اولیه فقط برای کالاهای از قبل تعریف‌شده امکان‌پذیر است.",
                        'value' => $sku ?: $barcode,
                    ];
                } else {
                    $matchedProduct = $candidates->first();
                }
            }

            // 2. Resolve Warehouse (Tenant-isolated)
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
                $rowErrors[] = [
                    'field' => 'warehouse',
                    'code' => ImportErrorCode::WAREHOUSE_NOT_FOUND,
                    'message' => ($warehouseVal !== '')
                        ? "انبار با مشخصات '{$warehouseVal}' یافت نشد."
                        : 'انبار مقصد مشخص نشده است و انبار پیش‌فرضی نیز برای عملیات انتخاب نشده است.',
                    'value' => (string) $warehouseRaw,
                ];
            }

            // 3. Validate Quantity (must be positive integer > 0)
            $qtyNorm = DataNormalizer::cleanString(DataNormalizer::toEnglishDigits($quantityRaw ?? ''));
            $parsedQty = null;
            if ($qtyNorm === '' || !ctype_digit($qtyNorm) || (int) $qtyNorm <= 0) {
                $rowErrors[] = [
                    'field' => 'quantity',
                    'code' => ImportErrorCode::INVALID_QUANTITY,
                    'message' => 'مقدار موجودی باید عدد صحیح مثبت (بزرگتر از صفر) باشد.',
                    'value' => (string) $quantityRaw,
                ];
            } else {
                $parsedQty = (int) $qtyNorm;
            }

            // 4. Validate Unit Cost (Optional)
            $parsedUnitCost = null;
            if ($unitCostRaw !== null && trim((string) $unitCostRaw) !== '') {
                $parsedUnitCost = DataNormalizer::parseMoney($unitCostRaw);
                if ($parsedUnitCost === null) {
                    $rowErrors[] = [
                        'field' => 'unit_cost',
                        'code' => ImportErrorCode::NUMERIC_INVALID,
                        'message' => 'بهای تمام شده / قیمت خرید نامعتبر است.',
                        'value' => (string) $unitCostRaw,
                    ];
                }
            }

            // 5. Check in-file duplicate (Product + Warehouse pair)
            if ($matchedProduct && $resolvedWarehouse) {
                $pairKey = "{$matchedProduct->id}:{$resolvedWarehouse->id}";
                if (isset($seenItemWarehouseInFile[$pairKey])) {
                    $rowErrors[] = [
                        'field' => 'sku',
                        'code' => ImportErrorCode::DUPLICATE_IN_FILE,
                        'message' => "موجودی کالا '{$matchedProduct->name}' برای انبار '{$resolvedWarehouse->name}' در ردیف {$seenItemWarehouseInFile[$pairKey]} نیز تکرار شده است.",
                        'value' => $sku ?: $barcode,
                    ];
                } else {
                    $seenItemWarehouseInFile[$pairKey] = $rowNumber;
                }

                // 6. Check existing database balance
                $existingStock = WarehouseStock::where('organization_id', $job->organization_id)
                    ->where('warehouse_id', $resolvedWarehouse->id)
                    ->where('product_id', $matchedProduct->id)
                    ->first();

                if ($existingStock && $existingStock->quantity > 0) {
                    $matchedExistingRows++;
                    $rowWarnings[] = [
                        'field' => 'quantity',
                        'code' => ImportErrorCode::EXISTING_BALANCE_CONFLICT,
                        'message' => "این کالا در انبار '{$resolvedWarehouse->name}' دارای موجودی قبلی ({$existingStock->quantity}) است.",
                        'value' => (string) $existingStock->quantity,
                    ];
                }
            }

            $isRowValid = empty($rowErrors);
            if ($isRowValid) {
                $validRows++;
            } else {
                $invalidRows++;
            }

            // Record DB validation results
            foreach ($rowErrors as $err) {
                $validationResultsToInsert[] = [
                    'id' => (string) Str::uuid(),
                    'import_job_id' => $job->id,
                    'organization_id' => $job->organization_id,
                    'source_file_id' => $source->id,
                    'worksheet_name' => 'Sheet' . ($sheetIndex + 1),
                    'row_number' => $rowNumber,
                    'field_name' => $err['field'],
                    'original_value' => mb_substr($err['value'] ?? '', 0, 255),
                    'error_code' => $err['code'],
                    'message' => $err['message'],
                    'suggested_action' => 'مقدار این فیلد را در فایل منبع تصحیح نمایید.',
                    'severity' => 'error',
                    'is_eligible' => false,
                    'created_at' => now(),
                    'updated_at' => now(),
                ];
            }

            foreach ($rowWarnings as $warn) {
                $validationResultsToInsert[] = [
                    'id' => (string) Str::uuid(),
                    'import_job_id' => $job->id,
                    'organization_id' => $job->organization_id,
                    'source_file_id' => $source->id,
                    'worksheet_name' => 'Sheet' . ($sheetIndex + 1),
                    'row_number' => $rowNumber,
                    'field_name' => $warn['field'],
                    'original_value' => mb_substr($warn['value'] ?? '', 0, 255),
                    'error_code' => $warn['code'],
                    'message' => $warn['message'],
                    'suggested_action' => 'در مرحله اجرا می‌توانید نادیده گرفتن یا بروزرسانی موجودی را انتخاب کنید.',
                    'severity' => 'warning',
                    'is_eligible' => true,
                    'created_at' => now(),
                    'updated_at' => now(),
                ];
            }

            // Collect preview rows up to limit
            if (count($previewRows) < self::PREVIEW_LIMIT) {
                $previewRows[] = [
                    'row_number' => $rowNumber,
                    'is_valid' => $isRowValid,
                    'has_match' => !empty($rowWarnings),
                    'errors' => $rowErrors,
                    'warnings' => $rowWarnings,
                    'mapped_data' => [
                        'sku' => $sku,
                        'product_name' => $matchedProduct ? $matchedProduct->name : null,
                        'barcode' => $barcode,
                        'quantity' => $parsedQty,
                        'warehouse' => $resolvedWarehouse ? $resolvedWarehouse->name : $warehouseVal,
                        'warehouse_code' => $resolvedWarehouse ? $resolvedWarehouse->code : null,
                        'unit_cost' => $parsedUnitCost ?? ($matchedProduct ? $matchedProduct->cost_price : null),
                        'notes' => $notes,
                    ],
                ];
            }
        }

        // Chunk insert validation results
        foreach (array_chunk($validationResultsToInsert, 200) as $chunk) {
            ImportValidationResult::insert($chunk);
        }

        // Update Job state and preview metadata
        $existingMeta = $job->metadata ?? [];
        $existingMeta['preview_rows'] = $previewRows;
        $existingMeta['matched_existing_rows'] = $matchedExistingRows;

        $job->update([
            'total_rows' => $totalRows,
            'processed_rows' => $totalRows,
            'valid_rows' => $validRows,
            'invalid_rows' => $invalidRows,
            'metadata' => $existingMeta,
        ]);

        return [
            'total_rows' => $totalRows,
            'valid_rows' => $validRows,
            'invalid_rows' => $invalidRows,
            'matched_existing_rows' => $matchedExistingRows,
            'preview_count' => count($previewRows),
        ];
    }

    /**
     * Validate operating expense rows.
     */
    private function validateExpenses(
        ImportJob $job,
        ImportSource $source,
        string $filePath,
        int $sheetIndex,
        array $mapping
    ): array {
        $seenExpensesInFile = [];

        $totalRows = 0;
        $validRows = 0;
        $invalidRows = 0;
        $matchedExistingRows = 0;

        $previewRows = [];
        $validationResultsToInsert = [];

        $defaultStoreId = $job->mapping_config['store_id'] ?? null;
        $tenantStores = Store::where('organization_id', $job->organization_id)->get();
        $defaultStore = $defaultStoreId ? $tenantStores->firstWhere('id', $defaultStoreId) : $tenantStores->first();

        $rowGenerator = $this->readerService->streamRows($filePath, $source->file_format, $sheetIndex);

        foreach ($rowGenerator as $rowItem) {
            $rowNumber = $rowItem['row_number'];
            $totalRows++;

            $rowErrors = [];
            $rowWarnings = [];

            // Extract mapped fields
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

            // Seamless fallback: If title was not provided or empty, but notes/description was provided, use notes as title
            if ($title === '' && $notes !== '') {
                $title = mb_substr($notes, 0, 191, 'UTF-8');
            }

            // 1. Validate Title (required, max 191)
            if ($title === '') {
                $rowErrors[] = [
                    'field' => 'title',
                    'code' => ImportErrorCode::REQUIRED_FIELD_MISSING,
                    'message' => 'عنوان هزینه الزامی است.',
                    'value' => (string) $titleRaw,
                ];
            } elseif (mb_strlen($title, 'UTF-8') > 191) {
                $rowErrors[] = [
                    'field' => 'title',
                    'code' => ImportErrorCode::STRING_TOO_LONG,
                    'message' => 'طول عنوان هزینه بیش از حد مجاز (۱۹۱ کاراکتر) است.',
                    'value' => $title,
                ];
            }

            // 2. Validate Category (required, resolve to valid code)
            $resolvedCategory = null;
            if ($categoryVal === '') {
                $rowErrors[] = [
                    'field' => 'category',
                    'code' => ImportErrorCode::REQUIRED_FIELD_MISSING,
                    'message' => 'دسته‌بندی هزینه الزامی است.',
                    'value' => (string) $categoryRaw,
                ];
            } else {
                $resolvedCategory = DataNormalizer::resolveExpenseCategory($categoryVal);
                if (!$resolvedCategory) {
                    $rowErrors[] = [
                        'field' => 'category',
                        'code' => ImportErrorCode::INVALID_CATEGORY,
                        'message' => "دسته‌بندی '{$categoryVal}' نامعتبر است. دسته‌بندی باید یکی از موارد مجاز (اجاره، اینترنت، برق، آب، حمل‌ونقل، تبلیغات، حقوق، تعمیرات، خدمات، کارمزد، سایر) باشد.",
                        'value' => $categoryVal,
                    ];
                }
            }

            // 3. Validate Amount (required, numeric > 0, exact decimal)
            $parsedAmount = DataNormalizer::parseMoney($amountRaw);
            if ($parsedAmount === null || (float) $parsedAmount <= 0) {
                $rowErrors[] = [
                    'field' => 'amount',
                    'code' => ImportErrorCode::INVALID_AMOUNT,
                    'message' => 'مبلغ هزینه باید یک عدد مثبت بزرگتر از صفر باشد.',
                    'value' => (string) $amountRaw,
                ];
            }

            // 4. Validate Date (required, valid Shamsi/Gregorian/Excel format)
            $parsedDate = DataNormalizer::parseDate($dateRaw);
            if ($parsedDate === null) {
                $rowErrors[] = [
                    'field' => 'date',
                    'code' => ImportErrorCode::INVALID_DATE,
                    'message' => 'تاریخ هزینه نامعتبر است یا در محدوده مجاز قرار ندارد (فرمت مجاز: ۱۴۰۳/۰۷/۲۱ یا 2024-10-12).',
                    'value' => (string) $dateRaw,
                ];
            }

            // 5. Resolve Store (tenant isolated)
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
                $rowErrors[] = [
                    'field' => 'store',
                    'code' => ImportErrorCode::STORE_NOT_FOUND,
                    'message' => ($storeVal !== '')
                        ? "فروشگاه با مشخصات '{$storeVal}' در سامانه یافت نشد یا به این سازمان تعلق ندارد."
                        : 'فروشگاه مقصد مشخص نشده است و هیچ فروشگاه پیش‌فرضی نیز برای سازمان یافت نشد.',
                    'value' => (string) $storeRaw,
                ];
            }

            // 6. Check for duplicate row within file
            $fileDupKey = null;
            if ($resolvedStore && $parsedDate && $resolvedCategory && $parsedAmount && $title !== '') {
                $fileDupKey = "{$resolvedStore->id}_{$parsedDate}_{$resolvedCategory}_{$parsedAmount}_" . mb_strtolower($title, 'UTF-8');
                if (isset($seenExpensesInFile[$fileDupKey])) {
                    $rowWarnings[] = [
                        'field' => 'title',
                        'code' => ImportErrorCode::DUPLICATE_IN_FILE,
                        'message' => "این ردیف تکرار ردیف شماره {$seenExpensesInFile[$fileDupKey]} در همین فایل است (همان تاریخ، فروشگاه، دسته‌بندی، مبلغ و عنوان).",
                        'value' => $title,
                    ];
                } else {
                    $seenExpensesInFile[$fileDupKey] = $rowNumber;
                }
            }

            // 7. Check for match against existing Expense record in database
            $hasExistingMatch = false;
            if (empty($rowErrors) && $resolvedStore && $parsedDate && $resolvedCategory && $parsedAmount && $title !== '') {
                $existingExpense = Expense::where('organization_id', $job->organization_id)
                    ->where('store_id', $resolvedStore->id)
                    ->where('date', $parsedDate)
                    ->where('category', $resolvedCategory)
                    ->where('amount', $parsedAmount)
                    ->whereRaw('LOWER(title) = ?', [mb_strtolower($title, 'UTF-8')])
                    ->first();

                if ($existingExpense) {
                    $hasExistingMatch = true;
                    $matchedExistingRows++;
                    $rowWarnings[] = [
                        'field' => 'title',
                        'code' => ImportErrorCode::EXISTING_RECORD_MATCH,
                        'message' => "هزینه مشابهی با همین تاریخ، دسته‌بندی ({$resolvedCategory})، مبلغ ({$parsedAmount}) و عنوان در فروشگاه '{$resolvedStore->name}' قبلاً ثبت شده است (شناسه: {$existingExpense->id}).",
                        'value' => $title,
                    ];
                }
            }

            $isValid = empty($rowErrors);
            if ($isValid) {
                $validRows++;
            } else {
                $invalidRows++;
            }

            // Record validation findings
            foreach ($rowErrors as $err) {
                $validationResultsToInsert[] = [
                    'id' => (string) Str::uuid(),
                    'import_job_id' => $job->id,
                    'organization_id' => $job->organization_id,
                    'row_number' => $rowNumber,
                    'field_name' => $err['field'],
                    'original_value' => $err['value'] ?? null,
                    'error_code' => $err['code'],
                    'message' => $err['message'],
                    'severity' => 'error',
                    'is_eligible' => false,
                    'created_at' => now(),
                    'updated_at' => now(),
                ];
            }

            foreach ($rowWarnings as $warn) {
                $validationResultsToInsert[] = [
                    'id' => (string) Str::uuid(),
                    'import_job_id' => $job->id,
                    'organization_id' => $job->organization_id,
                    'row_number' => $rowNumber,
                    'field_name' => $warn['field'],
                    'original_value' => $warn['value'] ?? null,
                    'error_code' => $warn['code'],
                    'message' => $warn['message'],
                    'severity' => 'warning',
                    'is_eligible' => true,
                    'created_at' => now(),
                    'updated_at' => now(),
                ];
            }

            // Collect preview row (up to limit)
            if (count($previewRows) < self::PREVIEW_LIMIT) {
                $previewRows[] = [
                    'row_number' => $rowNumber,
                    'is_valid' => $isValid,
                    'has_match' => $hasExistingMatch,
                    'errors' => $rowErrors,
                    'warnings' => $rowWarnings,
                    'mapped_data' => [
                        'title' => $title,
                        'category' => $resolvedCategory ?? $categoryVal,
                        'category_label' => $resolvedCategory ? Expense::categoryLabel($resolvedCategory) : $categoryVal,
                        'amount' => $parsedAmount ?? $amountRaw,
                        'date' => $parsedDate ?? $dateRaw,
                        'store' => $resolvedStore ? $resolvedStore->name : ($storeVal ?: 'پیش‌فرض'),
                        'payment_method' => $paymentMethod,
                        'notes' => $notes,
                    ],
                ];
            }

            // Flush validation results periodically
            if (count($validationResultsToInsert) >= 200) {
                ImportValidationResult::insert($validationResultsToInsert);
                $validationResultsToInsert = [];
            }
        }

        if (!empty($validationResultsToInsert)) {
            ImportValidationResult::insert($validationResultsToInsert);
        }

        $existingMeta = $job->metadata ?? [];
        $existingMeta['preview_rows'] = $previewRows;
        $existingMeta['matched_existing_rows'] = $matchedExistingRows;

        $job->update([
            'total_rows' => $totalRows,
            'processed_rows' => $totalRows,
            'valid_rows' => $validRows,
            'invalid_rows' => $invalidRows,
            'metadata' => $existingMeta,
        ]);

        return [
            'total_rows' => $totalRows,
            'valid_rows' => $validRows,
            'invalid_rows' => $invalidRows,
            'matched_existing_rows' => $matchedExistingRows,
        ];
    }

    /**
     * Validate historical order rows with multi-line grouping.
     */
    private function validateOrders(
        ImportJob $job,
        ImportSource $source,
        string $filePath,
        int $sheetIndex,
        array $mapping
    ): array {
        $totalRows = 0;
        $validRows = 0;
        $invalidRows = 0;
        $matchedExistingRows = 0;

        $previewRows = [];
        $validationResultsToInsert = [];

        $defaultStoreId = $job->mapping_config['store_id'] ?? null;
        $tenantStores = Store::where('organization_id', $job->organization_id)->get();
        $defaultStore = $defaultStoreId ? $tenantStores->firstWhere('id', $defaultStoreId) : $tenantStores->first();

        $defaultWarehouseId = $job->mapping_config['warehouse_id'] ?? null;
        $tenantWarehouses = Warehouse::where('organization_id', $job->organization_id)->get();
        $defaultWarehouse = $defaultWarehouseId ? $tenantWarehouses->firstWhere('id', $defaultWarehouseId) : $tenantWarehouses->first();

        $tenantProducts = Product::where('organization_id', $job->organization_id)->get();
        $tenantCustomers = Customer::where('organization_id', $job->organization_id)->get();

        // 1. Group rows by order_id
        $groupedOrders = [];
        $rowGenerator = $this->readerService->streamRows($filePath, $source->file_format, $sheetIndex);

        foreach ($rowGenerator as $rowItem) {
            $rowNumber = $rowItem['row_number'];
            $totalRows++;

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

            // Fallback for missing order_id: if empty, create row-specific ID so it can be evaluated
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

        // 2. Validate grouped orders
        foreach ($groupedOrders as $groupKey => $orderRows) {
            $isOrderLevelValid = true;
            $firstRow = $orderRows[0];
            $orderId = $firstRow['order_id'];

            // Header attributes from first row
            $dateParsed = DataNormalizer::parseDate($firstRow['date_raw']);
            $customerRaw = DataNormalizer::cleanString($firstRow['cust_raw'] ?? '');
            $storeRaw = DataNormalizer::cleanString($firstRow['store_raw'] ?? '');
            $paymentMethod = DataNormalizer::cleanString($firstRow['pay_raw'] ?? '') ?: 'CASH';
            $statusRaw = DataNormalizer::cleanString($firstRow['status_raw'] ?? '') ?: 'COMPLETED';
            $status = strtoupper($statusRaw);
            $notes = DataNormalizer::cleanString($firstRow['notes_raw'] ?? '');

            // Resolve Store
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

            // Resolve Customer (optional)
            $resolvedCustomer = null;
            if ($customerRaw !== '') {
                $resolvedCustomer = $tenantCustomers->first(function ($c) use ($customerRaw) {
                    return $c->id === $customerRaw
                        || mb_strtolower($c->name ?? '', 'UTF-8') === mb_strtolower($customerRaw, 'UTF-8')
                        || ($c->phone && $c->phone === DataNormalizer::normalizePhone($customerRaw));
                });
            }

            // Check if order already exists in database (by external_order_id or order_number)
            $existingOrder = null;
            if ($orderId !== '') {
                $existingOrder = Order::where('organization_id', $job->organization_id)
                    ->where(function ($q) use ($orderId) {
                        $q->where('external_order_id', $orderId)
                          ->orWhere('order_number', $orderId);
                    })
                    ->first();
            }

            $orderCalculatedSubtotalMinor = 0;
            $orderCalculatedDiscountMinor = 0;
            $orderCalculatedTaxMinor = 0;

            // Validate each row of the order
            foreach ($orderRows as $rowItem) {
                $rowNumber = $rowItem['row_number'];
                $rowErrors = [];
                $rowWarnings = [];

                // 2.1 Order ID check
                if ($rowItem['order_id'] === '') {
                    $rowErrors[] = [
                        'field' => 'order_id',
                        'code' => ImportErrorCode::ORDER_ID_REQUIRED,
                        'message' => 'شناسه سفارش خارجی برای این ردیف مشخص نشده است.',
                        'value' => (string) $rowItem['order_id'],
                    ];
                }

                // 2.2 Date check
                $rowDateParsed = DataNormalizer::parseDate($rowItem['date_raw']);
                if ($rowDateParsed === null) {
                    $rowErrors[] = [
                        'field' => 'date',
                        'code' => ImportErrorCode::INVALID_DATE,
                        'message' => 'تاریخ سفارش نامعتبر است (فرمت معتبر: ۱۴۰۲/۰۵/۱۵ یا 2023-08-06).',
                        'value' => (string) $rowItem['date_raw'],
                    ];
                } elseif ($dateParsed && $rowDateParsed !== $dateParsed) {
                    $rowErrors[] = [
                        'field' => 'date',
                        'code' => ImportErrorCode::ORDER_LINE_INCONSISTENCY,
                        'message' => "تاریخ این قلم ({$rowDateParsed}) با تاریخ سایر اقلام سفارش ({$dateParsed}) تناقض دارد.",
                        'value' => (string) $rowItem['date_raw'],
                    ];
                }

                // 2.3 Store check
                if (!$resolvedStore) {
                    $rowErrors[] = [
                        'field' => 'store',
                        'code' => ImportErrorCode::STORE_NOT_FOUND,
                        'message' => 'فروشگاه معتبری برای این سفارش مشخص نشده است.',
                        'value' => (string) $rowItem['store_raw'],
                    ];
                }

                // 2.4 Status check
                if (!in_array($status, ['COMPLETED', 'NEW', 'PREPARING', 'CANCELLED'], true)) {
                    $rowErrors[] = [
                        'field' => 'status',
                        'code' => ImportErrorCode::INVALID_ORDER_STATUS,
                        'message' => "وضعیت سفارش '{$status}' نامعتبر است.",
                        'value' => (string) $rowItem['status_raw'],
                    ];
                }

                // 2.5 SKU / Product check
                $skuNorm = DataNormalizer::cleanString(DataNormalizer::toEnglishDigits($rowItem['sku_raw'] ?? ''));
                $matchedProduct = null;
                if ($skuNorm === '') {
                    $rowErrors[] = [
                        'field' => 'sku',
                        'code' => ImportErrorCode::REQUIRED_FIELD_MISSING,
                        'message' => 'کد یا بارکد کالا الزامی است.',
                        'value' => (string) $rowItem['sku_raw'],
                    ];
                } else {
                    $matchedProduct = $tenantProducts->first(function ($p) use ($skuNorm) {
                        return (string) $p->sku === $skuNorm
                            || (string) $p->barcode === $skuNorm
                            || (string) $p->id === $skuNorm;
                    });

                    if (!$matchedProduct) {
                        $rowErrors[] = [
                            'field' => 'sku',
                            'code' => ImportErrorCode::PRODUCT_NOT_FOUND,
                            'message' => "کالایی با شناسه یا بارکد '{$skuNorm}' در سامانه یافت نشد.",
                            'value' => $skuNorm,
                        ];
                    }
                }

                // 2.6 Quantity check (positive integer)
                $qtyNorm = DataNormalizer::cleanString(DataNormalizer::toEnglishDigits($rowItem['qty_raw'] ?? ''));
                $qty = 0;
                if ($qtyNorm === '' || !ctype_digit($qtyNorm) || (int) $qtyNorm <= 0) {
                    $rowErrors[] = [
                        'field' => 'quantity',
                        'code' => ImportErrorCode::INVALID_QUANTITY,
                        'message' => 'تعداد کالا باید عدد صحیح مثبت (بزرگتر از صفر) باشد.',
                        'value' => (string) $rowItem['qty_raw'],
                    ];
                } else {
                    $qty = (int) $qtyNorm;
                }

                // 2.7 Unit price check (exact money >= 0)
                $priceNorm = DataNormalizer::parseMoney($rowItem['price_raw']);
                if ($priceNorm === null || (float) $priceNorm < 0) {
                    $rowErrors[] = [
                        'field' => 'unit_price',
                        'code' => ImportErrorCode::NUMERIC_INVALID,
                        'message' => 'قیمت واحد کالا نامعتبر است.',
                        'value' => (string) $rowItem['price_raw'],
                    ];
                }

                // Accumulate line totals for mathematical consistency check
                if ($qty > 0 && $priceNorm !== null) {
                    $lineSubtotalMinor = Money::multiply(Money::toMinor($priceNorm), $qty);
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

                    $orderCalculatedSubtotalMinor += $lineSubtotalMinor;
                    $orderCalculatedDiscountMinor += $lineDiscMinor;
                    $orderCalculatedTaxMinor += $lineTaxMinor;
                }

                // 2.8 Existing Order Match warning
                if ($existingOrder) {
                    $rowWarnings[] = [
                        'field' => 'order_id',
                        'code' => ImportErrorCode::EXISTING_RECORD_MATCH,
                        'message' => "سفارشی با شناسه '{$orderId}' از قبل در سامانه ثبت شده است (#{$existingOrder->order_number}).",
                        'value' => $orderId,
                    ];
                }

                $isRowValid = empty($rowErrors);
                if ($isRowValid) {
                    $validRows++;
                } else {
                    $invalidRows++;
                    $isOrderLevelValid = false;
                }

                if ($existingOrder) {
                    $matchedExistingRows++;
                }

                // Record DB validation results
                foreach ($rowErrors as $err) {
                    $validationResultsToInsert[] = [
                        'id' => (string) Str::uuid(),
                        'import_job_id' => $job->id,
                        'organization_id' => $job->organization_id,
                        'row_number' => $rowNumber,
                        'field_name' => $err['field'],
                        'original_value' => $err['value'] ?? null,
                        'error_code' => $err['code'],
                        'message' => $err['message'],
                        'severity' => 'error',
                        'is_eligible' => false,
                        'created_at' => now(),
                        'updated_at' => now(),
                    ];
                }

                foreach ($rowWarnings as $warn) {
                    $validationResultsToInsert[] = [
                        'id' => (string) Str::uuid(),
                        'import_job_id' => $job->id,
                        'organization_id' => $job->organization_id,
                        'row_number' => $rowNumber,
                        'field_name' => $warn['field'],
                        'original_value' => $warn['value'] ?? null,
                        'error_code' => $warn['code'],
                        'message' => $warn['message'],
                        'severity' => 'warning',
                        'is_eligible' => true,
                        'created_at' => now(),
                        'updated_at' => now(),
                    ];
                }

                // Collect preview row up to limit
                if (count($previewRows) < self::PREVIEW_LIMIT) {
                    $previewRows[] = [
                        'row_number' => $rowNumber,
                        'is_valid' => $isRowValid,
                        'has_match' => !empty($existingOrder),
                        'errors' => $rowErrors,
                        'warnings' => $rowWarnings,
                        'mapped_data' => [
                            'order_id' => $orderId,
                            'sku' => $skuNorm,
                            'product_name' => $matchedProduct ? $matchedProduct->name : null,
                            'quantity' => $qty,
                            'unit_price' => $priceNorm,
                            'date' => $rowDateParsed ?? $rowItem['date_raw'],
                            'customer' => $resolvedCustomer ? $resolvedCustomer->name : ($customerRaw ?: 'مشتری حضوری'),
                            'store' => $resolvedStore ? $resolvedStore->name : ($storeRaw ?: 'پیش‌فرض'),
                            'payment_method' => $paymentMethod,
                            'status' => $status,
                            'total_amount' => $firstRow['total_raw'] ? DataNormalizer::parseMoney($firstRow['total_raw']) : null,
                        ],
                    ];
                }

                if (count($validationResultsToInsert) >= 200) {
                    ImportValidationResult::insert($validationResultsToInsert);
                    $validationResultsToInsert = [];
                }
            }

            // 2.9 Order total mathematical check (if total was provided in file)
            if ($firstRow['total_raw'] !== null && trim((string) $firstRow['total_raw']) !== '') {
                $expectedTotalMinor = Money::toMinor(DataNormalizer::parseMoney($firstRow['total_raw']) ?? '0');
                $calculatedTotalMinor = $orderCalculatedSubtotalMinor - $orderCalculatedDiscountMinor + $orderCalculatedTaxMinor;

                if ($expectedTotalMinor > 0 && abs($expectedTotalMinor - $calculatedTotalMinor) > 5) {
                    // Difference > 0.05 currency units -> flag mismatch
                    $validationResultsToInsert[] = [
                        'id' => (string) Str::uuid(),
                        'import_job_id' => $job->id,
                        'organization_id' => $job->organization_id,
                        'row_number' => $firstRow['row_number'],
                        'field_name' => 'total_amount',
                        'original_value' => (string) $firstRow['total_raw'],
                        'error_code' => ImportErrorCode::ORDER_TOTAL_MISMATCH,
                        'message' => "جمع مبالغ اقلام (" . Money::fromMinor($calculatedTotalMinor) . ") با مبلغ کل فاکتور (" . Money::fromMinor($expectedTotalMinor) . ") همخوانی ندارد.",
                        'severity' => 'warning',
                        'is_eligible' => true,
                        'created_at' => now(),
                        'updated_at' => now(),
                    ];
                }
            }
        }

        if (!empty($validationResultsToInsert)) {
            ImportValidationResult::insert($validationResultsToInsert);
        }

        $existingMeta = $job->metadata ?? [];
        $existingMeta['preview_rows'] = $previewRows;
        $existingMeta['matched_existing_rows'] = $matchedExistingRows;

        $job->update([
            'total_rows' => $totalRows,
            'processed_rows' => $totalRows,
            'valid_rows' => $validRows,
            'invalid_rows' => $invalidRows,
            'metadata' => $existingMeta,
        ]);

        return [
            'total_rows' => $totalRows,
            'valid_rows' => $validRows,
            'invalid_rows' => $invalidRows,
            'matched_existing_rows' => $matchedExistingRows,
            'preview_count' => count($previewRows),
        ];
    }

    private function getMappedValue(array $rowItem, array $mapping, string $targetField): ?string
    {
        $sourceCol = $mapping[$targetField] ?? null;
        if ($sourceCol === null || $sourceCol === '') {
            return null;
        }

        // If mapped by column name in associative array
        if (isset($rowItem['assoc'][$sourceCol])) {
            return $rowItem['assoc'][$sourceCol];
        }

        // If mapped by index
        if (is_numeric($sourceCol) && isset($rowItem['row'][(int) $sourceCol])) {
            return $rowItem['row'][(int) $sourceCol];
        }

        return null;
    }
}
