<?php

namespace App\Services\Import\Validation;

use App\Models\Customer;
use App\Models\ImportDataset;
use App\Models\ImportJob;
use App\Models\ImportSource;
use App\Models\ImportValidationResult;
use App\Models\Product;
use App\Models\Warehouse;
use App\Models\WarehouseStock;
use App\Services\Import\ImportStateMachine;
use App\Services\Import\Mapping\ImportMappingService;
use App\Services\Import\Normalizer\DataNormalizer;
use App\Services\Import\Reader\SpreadsheetReaderService;
use App\Support\Import\ImportErrorCode;
use App\Support\Import\ImportStatus;
use App\Support\Import\ImportType;
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
