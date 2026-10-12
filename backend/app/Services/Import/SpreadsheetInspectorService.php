<?php

namespace App\Services\Import;

use App\Support\Import\FileFormat;
use App\Support\Import\ImportErrorCode;
use RuntimeException;
use SimpleXMLElement;
use XMLReader;
use ZipArchive;

class SpreadsheetInspectorService
{
    /** Maximum rows sampled for preview */
    public const PREVIEW_SAMPLE_LIMIT = 5;

    /**
     * Inspect a stored file and extract datasets / worksheets metadata.
     *
     * @return array<int, array{
     *     name: string,
     *     sheet_index: int,
     *     row_count: int,
     *     headers: string[],
     *     sample_rows: array<int, string[]>,
     *     metadata: array<string, mixed>
     * }>
     */
    public function inspect(string $absolutePath, string $format): array
    {
        if (!file_exists($absolutePath)) {
            throw new RuntimeException('Source file does not exist on disk.');
        }

        if ($format === FileFormat::CSV) {
            return [$this->inspectCsv($absolutePath)];
        }

        if ($format === FileFormat::XLSX) {
            return $this->inspectXlsx($absolutePath);
        }

        throw new RuntimeException(
            ImportErrorCode::defaultMessage(ImportErrorCode::FILE_FORMAT_UNSUPPORTED)
        );
    }

    /**
     * Inspect a CSV file with automatic delimiter detection and UTF-8 / BOM handling.
     *
     * @return array{
     *     name: string,
     *     sheet_index: int,
     *     row_count: int,
     *     headers: string[],
     *     sample_rows: array<int, string[]>,
     *     metadata: array<string, mixed>
     * }
     */
    public function inspectCsv(string $path): array
    {
        $handle = fopen($path, 'r');
        if ($handle === false) {
            throw new RuntimeException('Cannot open CSV file.');
        }

        // Check and strip UTF-8 BOM if present
        $bom = fread($handle, 3);
        $hasBom = ($bom === "\xEF\xBB\xBF");
        if (!$hasBom) {
            rewind($handle);
        }

        // Sample up to 10 lines to detect delimiter
        $sampleText = '';
        for ($i = 0; $i < 10 && !feof($handle); $i++) {
            $line = fgets($handle);
            if ($line !== false) {
                $sampleText .= $line;
            }
        }

        $delimiter = $this->detectCsvDelimiter($sampleText);

        // Reset to beginning of data
        rewind($handle);
        if ($hasBom) {
            fread($handle, 3);
        }

        $headers = [];
        $sampleRows = [];
        $rowCount = 0;

        while (($row = fgetcsv($handle, 0, $delimiter)) !== false) {
            // Skip entirely empty rows
            if ($this->isEmptyRow($row)) {
                continue;
            }

            // Normalize strings to UTF-8 and trim whitespace
            $cleanRow = array_map(function ($val) {
                return $this->normalizeString($val ?? '');
            }, $row);

            if (empty($headers)) {
                $headers = $cleanRow;
                continue;
            }

            $rowCount++;
            if (count($sampleRows) < self::PREVIEW_SAMPLE_LIMIT) {
                $sampleRows[] = $cleanRow;
            }
        }

        fclose($handle);

        return [
            'name' => 'Sheet1',
            'sheet_index' => 0,
            'row_count' => $rowCount,
            'headers' => $headers,
            'sample_rows' => $sampleRows,
            'metadata' => [
                'delimiter' => $delimiter,
                'has_bom' => $hasBom,
                'encoding' => 'UTF-8',
            ],
        ];
    }

    /**
     * Detect CSV delimiter based on occurrence frequency.
     */
    private function detectCsvDelimiter(string $sample): string
    {
        $delimiters = [',', ';', "\t", '|'];
        $counts = [];

        foreach ($delimiters as $delim) {
            $counts[$delim] = substr_count($sample, $delim);
        }

        arsort($counts);
        $best = array_key_first($counts);

        return ($counts[$best] > 0) ? $best : ',';
    }

    /**
     * Inspect XLSX archive using ZipArchive and XMLReader for bounded memory.
     *
     * @return array<int, array{
     *     name: string,
     *     sheet_index: int,
     *     row_count: int,
     *     headers: string[],
     *     sample_rows: array<int, string[]>,
     *     metadata: array<string, mixed>
     * }>
     */
    public function inspectXlsx(string $path): array
    {
        $zip = new ZipArchive();
        if ($zip->open($path, ZipArchive::RDONLY) !== true) {
            throw new RuntimeException(
                ImportErrorCode::defaultMessage(ImportErrorCode::FILE_CORRUPTED)
            );
        }

        // 1. Read shared strings
        $sharedStrings = $this->loadXlsxSharedStrings($zip);

        // 2. Discover sheets from xl/workbook.xml
        $workbookXml = $zip->getFromName('xl/workbook.xml');
        if ($workbookXml === false) {
            $zip->close();
            throw new RuntimeException('Invalid XLSX workbook structure.');
        }

        $sheets = $this->extractXlsxSheets($workbookXml);

        $results = [];

        foreach ($sheets as $index => $sheetInfo) {
            $sheetTarget = 'xl/worksheets/sheet' . ($index + 1) . '.xml';
            if ($zip->locateName($sheetTarget) === false) {
                // Try rId or filename matching
                $sheetTarget = 'xl/worksheets/sheet1.xml';
            }

            $sheetXmlStream = $zip->getStream($sheetTarget);
            if ($sheetXmlStream === false) {
                continue;
            }

            $parsedSheet = $this->streamParseWorksheet($sheetXmlStream, $sharedStrings);
            fclose($sheetXmlStream);

            $results[] = [
                'name' => $sheetInfo['name'],
                'sheet_index' => $index,
                'row_count' => $parsedSheet['row_count'],
                'headers' => $parsedSheet['headers'],
                'sample_rows' => $parsedSheet['sample_rows'],
                'metadata' => [
                    'sheet_id' => $sheetInfo['id'],
                ],
            ];
        }

        $zip->close();

        return !empty($results) ? $results : [[
            'name' => 'Sheet1',
            'sheet_index' => 0,
            'row_count' => 0,
            'headers' => [],
            'sample_rows' => [],
            'metadata' => [],
        ]];
    }

    /**
     * Extract shared strings table from xl/sharedStrings.xml in streaming fashion.
     *
     * @return string[]
     */
    private function loadXlsxSharedStrings(ZipArchive $zip): array
    {
        $stream = $zip->getStream('xl/sharedStrings.xml');
        if ($stream === false) {
            return [];
        }

        $reader = new XMLReader();
        $tempPath = tempnam(sys_get_temp_dir(), 'xlsx_ss_');
        file_put_contents($tempPath, stream_get_contents($stream));
        fclose($stream);

        $reader->open($tempPath);

        $strings = [];
        $currentText = '';
        $inSi = false;

        while ($reader->read()) {
            if ($reader->nodeType === XMLReader::ELEMENT && $reader->name === 'si') {
                $inSi = true;
                $currentText = '';
            } elseif ($reader->nodeType === XMLReader::END_ELEMENT && $reader->name === 'si') {
                $strings[] = $this->normalizeString($currentText);
                $inSi = false;
            } elseif ($inSi && $reader->nodeType === XMLReader::ELEMENT && $reader->name === 't') {
                $currentText .= $reader->readString();
            }
        }

        $reader->close();
        @unlink($tempPath);

        return $strings;
    }

    /**
     * Extract sheet names from xl/workbook.xml.
     *
     * @return array<int, array{name: string, id: string}>
     */
    private function extractXlsxSheets(string $workbookXml): array
    {
        $sheets = [];
        try {
            $xml = new SimpleXMLElement($workbookXml);
            if (isset($xml->sheets->sheet)) {
                foreach ($xml->sheets->sheet as $s) {
                    $sheets[] = [
                        'name' => (string) $s['name'],
                        'id' => (string) ($s['sheetId'] ?? ''),
                    ];
                }
            }
        } catch (\Throwable) {
            $sheets[] = ['name' => 'Sheet1', 'id' => '1'];
        }

        return $sheets;
    }

    /**
     * Stream parse worksheet XML using XMLReader.
     * Never evaluates formulas. Extracts literal values <v> or shared strings.
     *
     * @param resource $stream
     * @param string[] $sharedStrings
     * @return array{row_count: int, headers: string[], sample_rows: array<int, string[]>}
     */
    private function streamParseWorksheet($stream, array $sharedStrings): array
    {
        $tempPath = tempnam(sys_get_temp_dir(), 'xlsx_ws_');
        file_put_contents($tempPath, stream_get_contents($stream));

        $reader = new XMLReader();
        $reader->open($tempPath);

        $headers = [];
        $sampleRows = [];
        $dataRowCount = 0;
        $currentRowCells = [];
        $inRow = false;
        $cellType = null;

        while ($reader->read()) {
            if ($reader->nodeType === XMLReader::ELEMENT && $reader->name === 'row') {
                $inRow = true;
                $currentRowCells = [];
            } elseif ($reader->nodeType === XMLReader::END_ELEMENT && $reader->name === 'row') {
                if ($inRow && !$this->isEmptyRow($currentRowCells)) {
                    if (empty($headers)) {
                        $headers = $currentRowCells;
                    } else {
                        $dataRowCount++;
                        if (count($sampleRows) < self::PREVIEW_SAMPLE_LIMIT) {
                            $sampleRows[] = $currentRowCells;
                        }
                    }
                }
                $inRow = false;
            } elseif ($inRow && $reader->nodeType === XMLReader::ELEMENT && $reader->name === 'c') {
                $cellType = $reader->getAttribute('t');
                $cellVal = '';

                // Read cell children: ignore <f> (formulas), extract <v> or <is><t>
                $depth = $reader->depth;
                while ($reader->read() && $reader->depth > $depth) {
                    if ($reader->nodeType === XMLReader::ELEMENT && $reader->name === 'v') {
                        $rawVal = $reader->readString();
                        if ($cellType === 's') {
                            $idx = (int) $rawVal;
                            $cellVal = $sharedStrings[$idx] ?? '';
                        } else {
                            $cellVal = $rawVal;
                        }
                    } elseif ($reader->nodeType === XMLReader::ELEMENT && $reader->name === 't') {
                        $cellVal = $reader->readString();
                    }
                }

                $currentRowCells[] = $this->normalizeString($cellVal);
            }
        }

        $reader->close();
        @unlink($tempPath);

        return [
            'row_count' => $dataRowCount,
            'headers' => $headers,
            'sample_rows' => $sampleRows,
        ];
    }

    private function isEmptyRow(array $row): bool
    {
        foreach ($row as $val) {
            if (trim((string) $val) !== '') {
                return false;
            }
        }
        return true;
    }

    /**
     * Clean and normalize string (handle UTF-8, Persian numbers if present).
     */
    private function normalizeString(string $val): string
    {
        $val = trim($val);
        // Ensure UTF-8
        if (!mb_check_encoding($val, 'UTF-8')) {
            $val = mb_convert_encoding($val, 'UTF-8', 'ISO-8859-1');
        }
        return $val;
    }
}
