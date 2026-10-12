<?php

namespace App\Services\Import;

use App\Support\Import\FileFormat;
use App\Support\Import\ImportErrorCode;
use Illuminate\Http\UploadedFile;
use Illuminate\Support\Facades\Storage;
use Illuminate\Support\Str;
use InvalidArgumentException;
use RuntimeException;
use ZipArchive;

class ImportStorageService
{
    public const MAX_FILE_SIZE_BYTES = 20 * 1024 * 1024; // 20 MB

    public const ALLOWED_EXTENSIONS = ['xlsx', 'csv'];

    public const DISK = 'local';

    /**
     * Inspect and securely store an uploaded import file.
     *
     * @return array{
     *     storage_path: string,
     *     original_filename: string,
     *     file_format: string,
     *     file_size: int,
     *     checksum: string
     * }
     */
    public function storeUploadedFile(UploadedFile $file, string $organizationId): array
    {
        if (!$file->isValid()) {
            throw new InvalidArgumentException(
                ImportErrorCode::defaultMessage(ImportErrorCode::FILE_CORRUPTED)
            );
        }

        $size = $file->getSize();
        if ($size <= 0 || $size > self::MAX_FILE_SIZE_BYTES) {
            throw new InvalidArgumentException(
                ImportErrorCode::defaultMessage(ImportErrorCode::FILE_TOO_LARGE)
            );
        }

        $originalName = $file->getClientOriginalName();
        $extension = strtolower($file->getClientOriginalExtension());

        if (!in_array($extension, self::ALLOWED_EXTENSIONS, true)) {
            throw new InvalidArgumentException(
                ImportErrorCode::defaultMessage(ImportErrorCode::FILE_FORMAT_UNSUPPORTED)
            );
        }

        $realPath = $file->getRealPath();
        if (!$realPath || !file_exists($realPath)) {
            throw new RuntimeException(
                ImportErrorCode::defaultMessage(ImportErrorCode::FILE_CORRUPTED)
            );
        }

        // Deep content inspection
        $detectedFormat = $this->inspectFileContent($realPath, $extension);

        $checksum = hash_file('sha256', $realPath);
        if ($checksum === false) {
            throw new RuntimeException('Failed to compute file checksum.');
        }

        // Store file using a random UUID in private tenant directory
        $safeFileName = Str::uuid()->toString() . '.' . $detectedFormat;
        $tenantDir = 'imports/' . $organizationId;
        $relativeStoragePath = $tenantDir . '/' . $safeFileName;

        $stream = fopen($realPath, 'rb');
        if ($stream === false) {
            throw new RuntimeException('Failed to open source file for reading.');
        }

        $stored = Storage::disk(self::DISK)->put($relativeStoragePath, $stream);
        if (is_resource($stream)) {
            fclose($stream);
        }

        if (!$stored) {
            throw new RuntimeException('Failed to write file to private storage.');
        }

        return [
            'storage_path' => $relativeStoragePath,
            'original_filename' => basename($originalName),
            'file_format' => $detectedFormat,
            'file_size' => $size,
            'checksum' => $checksum,
        ];
    }

    /**
     * Inspect file content beyond extension and client mime type.
     */
    public function inspectFileContent(string $path, string $declaredExtension): string
    {
        $handle = fopen($path, 'rb');
        if ($handle === false) {
            throw new InvalidArgumentException(
                ImportErrorCode::defaultMessage(ImportErrorCode::FILE_CORRUPTED)
            );
        }

        $header = fread($handle, 512);
        fclose($handle);

        if ($header === false || strlen($header) === 0) {
            throw new InvalidArgumentException(
                ImportErrorCode::defaultMessage(ImportErrorCode::NO_DATA_FOUND)
            );
        }

        // Block executable binaries (ELF, PE, Mach-O)
        if (str_starts_with($header, "\x7fELF") ||
            str_starts_with($header, "MZ") ||
            str_starts_with($header, "\xfe\xed\xfa\xce") ||
            str_starts_with($header, "\xfe\xed\xfa\xcf") ||
            str_starts_with($header, "\xce\xfa\xed\xfe") ||
            str_starts_with($header, "\xcf\xfa\xed\xfe")
        ) {
            throw new InvalidArgumentException(
                ImportErrorCode::defaultMessage(ImportErrorCode::SUSPICIOUS_CONTENT)
            );
        }

        // Block embedded PHP or shell script signatures
        if (stripos($header, '<?php') !== false ||
            stripos($header, '<?=') !== false ||
            stripos($header, '<script') !== false ||
            str_starts_with($header, '#!/')
        ) {
            throw new InvalidArgumentException(
                ImportErrorCode::defaultMessage(ImportErrorCode::SUSPICIOUS_CONTENT)
            );
        }

        if ($declaredExtension === FileFormat::XLSX) {
            // Must have standard ZIP header: PK\x03\x04
            if (!str_starts_with($header, "PK\x03\x04")) {
                throw new InvalidArgumentException(
                    ImportErrorCode::defaultMessage(ImportErrorCode::FILE_FORMAT_UNSUPPORTED)
                );
            }

            // Verify zip integrity and reject macros
            $zip = new ZipArchive();
            $res = $zip->open($path, ZipArchive::RDONLY);
            if ($res !== true) {
                throw new InvalidArgumentException(
                    ImportErrorCode::defaultMessage(ImportErrorCode::FILE_CORRUPTED)
                );
            }

            $hasWorkbook = false;
            for ($i = 0; $i < $zip->numFiles; $i++) {
                $stat = $zip->statIndex($i);
                $name = $stat['name'] ?? '';

                // Reject any macro / VBA scripts
                if (stripos($name, 'vbaProject.bin') !== false ||
                    stripos($name, 'vbaData.xml') !== false ||
                    str_ends_with(strtolower($name), '.vbs') ||
                    str_ends_with(strtolower($name), '.exe')
                ) {
                    $zip->close();
                    throw new InvalidArgumentException(
                        ImportErrorCode::defaultMessage(ImportErrorCode::MACRO_DETECTED)
                    );
                }

                if (str_starts_with($name, 'xl/workbook.xml')) {
                    $hasWorkbook = true;
                }
            }

            $zip->close();

            if (!$hasWorkbook) {
                throw new InvalidArgumentException(
                    ImportErrorCode::defaultMessage(ImportErrorCode::FILE_FORMAT_UNSUPPORTED)
                );
            }

            return FileFormat::XLSX;
        }

        if ($declaredExtension === FileFormat::CSV) {
            // Null bytes are not permitted in plain CSV
            if (str_contains($header, "\x00")) {
                throw new InvalidArgumentException(
                    ImportErrorCode::defaultMessage(ImportErrorCode::SUSPICIOUS_CONTENT)
                );
            }

            return FileFormat::CSV;
        }

        throw new InvalidArgumentException(
            ImportErrorCode::defaultMessage(ImportErrorCode::FILE_FORMAT_UNSUPPORTED)
        );
    }

    /**
     * Read stream for download.
     */
    public function getStream(string $storagePath)
    {
        if (!Storage::disk(self::DISK)->exists($storagePath)) {
            throw new RuntimeException('File not found in storage.');
        }

        return Storage::disk(self::DISK)->readStream($storagePath);
    }

    /**
     * Delete stored file.
     */
    public function deleteFile(string $storagePath): bool
    {
        if (Storage::disk(self::DISK)->exists($storagePath)) {
            return Storage::disk(self::DISK)->delete($storagePath);
        }
        return true;
    }
}
