<?php

namespace App\Services\Import\Reader;

use App\Support\Import\FileFormat;
use App\Support\Import\ImportErrorCode;
use Generator;
use RuntimeException;
use SimpleXMLElement;
use XMLReader;
use ZipArchive;

class SpreadsheetReaderService
{
    /**
     * Stream rows from a file (CSV or XLSX).
     * Yields array of:
     * [
     *     'row_number' => int (1-based data row index, header is not yielded or row_number starts at 2),
     *     'row' => array<int, string>, // indexed by column index 0..N
     *     'assoc' => array<string, string>, // mapped by header name if headers available
     * ]
     *
     * @return Generator<int, array{row_number: int, row: string[], assoc: array<string, string>}>
     */
    public function streamRows(string $absolutePath, string $format, int $sheetIndex = 0): Generator
    {
        if (!file_exists($absolutePath)) {
            throw new RuntimeException('Source file does not exist on disk.');
        }

        if ($format === FileFormat::CSV) {
            yield from $this->streamCsv($absolutePath);
            return;
        }

        if ($format === FileFormat::XLSX) {
            yield from $this->streamXlsx($absolutePath, $sheetIndex);
            return;
        }

        throw new RuntimeException(
            ImportErrorCode::defaultMessage(ImportErrorCode::FILE_FORMAT_UNSUPPORTED)
        );
    }

    /**
     * Stream rows from CSV.
     */
    private function streamCsv(string $path): Generator
    {
        $handle = fopen($path, 'r');
        if ($handle === false) {
            throw new RuntimeException('Cannot open CSV file.');
        }

        $bom = fread($handle, 3);
        $hasBom = ($bom === "\xEF\xBB\xBF");
        if (!$hasBom) {
            rewind($handle);
        }

        // Detect delimiter
        $sampleText = '';
        for ($i = 0; $i < 10 && !feof($handle); $i++) {
            $line = fgets($handle);
            if ($line !== false) {
                $sampleText .= $line;
            }
        }
        $delimiter = $this->detectCsvDelimiter($sampleText);

        rewind($handle);
        if ($hasBom) {
            fread($handle, 3);
        }

        $headers = [];
        $fileLineNumber = 0;
        $dataRowIndex = 0;

        while (($row = fgetcsv($handle, 0, $delimiter)) !== false) {
            $fileLineNumber++;

            if ($this->isEmptyRow($row)) {
                continue;
            }

            $cleanRow = array_map(function ($val) {
                return $this->normalizeString($val ?? '');
            }, $row);

            if (empty($headers)) {
                $headers = $cleanRow;
                continue;
            }

            $dataRowIndex++;

            $assoc = [];
            foreach ($headers as $colIdx => $headerName) {
                $assoc[$headerName] = $cleanRow[$colIdx] ?? '';
            }

            yield [
                'row_number' => $fileLineNumber,
                'data_row_index' => $dataRowIndex,
                'row' => $cleanRow,
                'assoc' => $assoc,
            ];
        }

        fclose($handle);
    }

    /**
     * Stream rows from XLSX.
     */
    private function streamXlsx(string $path, int $targetSheetIndex = 0): Generator
    {
        $zip = new ZipArchive();
        if ($zip->open($path, ZipArchive::RDONLY) !== true) {
            throw new RuntimeException(
                ImportErrorCode::defaultMessage(ImportErrorCode::FILE_CORRUPTED)
            );
        }

        $sharedStrings = $this->loadXlsxSharedStrings($zip);

        $sheetTarget = 'xl/worksheets/sheet' . ($targetSheetIndex + 1) . '.xml';
        if ($zip->locateName($sheetTarget) === false) {
            $sheetTarget = 'xl/worksheets/sheet1.xml';
        }

        $sheetXmlStream = $zip->getStream($sheetTarget);
        if ($sheetXmlStream === false) {
            $zip->close();
            throw new RuntimeException("Worksheet at index {$targetSheetIndex} not found in workbook.");
        }

        $tempPath = tempnam(sys_get_temp_dir(), 'xlsx_read_');
        file_put_contents($tempPath, stream_get_contents($sheetXmlStream));
        fclose($sheetXmlStream);
        $zip->close();

        $reader = new XMLReader();
        $reader->open($tempPath);

        $headers = [];
        $inRow = false;
        $fileLineNumber = 0;
        $dataRowIndex = 0;
        $currentRowCells = [];

        while ($reader->read()) {
            if ($reader->nodeType === XMLReader::ELEMENT && $reader->name === 'row') {
                $inRow = true;
                $currentRowCells = [];
                $rowAttr = $reader->getAttribute('r');
                $fileLineNumber = $rowAttr ? (int) $rowAttr : ($fileLineNumber + 1);
            } elseif ($reader->nodeType === XMLReader::END_ELEMENT && $reader->name === 'row') {
                if ($inRow && !$this->isEmptyRow($currentRowCells)) {
                    if (empty($headers)) {
                        $headers = $currentRowCells;
                    } else {
                        $dataRowIndex++;
                        $assoc = [];
                        foreach ($headers as $colIdx => $headerName) {
                            $assoc[$headerName] = $currentRowCells[$colIdx] ?? '';
                        }

                        yield [
                            'row_number' => $fileLineNumber,
                            'data_row_index' => $dataRowIndex,
                            'row' => $currentRowCells,
                            'assoc' => $assoc,
                        ];
                    }
                }
                $inRow = false;
            } elseif ($inRow && $reader->nodeType === XMLReader::ELEMENT && $reader->name === 'c') {
                $cellType = $reader->getAttribute('t');
                $cellRef = $reader->getAttribute('r'); // e.g. A1, B1, C2
                $cellColIdx = $cellRef ? $this->columnLetterToIndex($cellRef) : count($currentRowCells);

                // Pad empty cells if columns were skipped
                while (count($currentRowCells) < $cellColIdx) {
                    $currentRowCells[] = '';
                }

                $cellVal = '';
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

                $currentRowCells[$cellColIdx] = $this->normalizeString($cellVal);
            }
        }

        $reader->close();
        @unlink($tempPath);
    }

    private function columnLetterToIndex(string $cellRef): int
    {
        $letters = preg_replace('/[^A-Z]/', '', strtoupper($cellRef));
        $len = strlen($letters);
        $index = 0;
        for ($i = 0; $i < $len; $i++) {
            $index = $index * 26 + (ord($letters[$i]) - 64);
        }
        return max(0, $index - 1);
    }

    private function loadXlsxSharedStrings(ZipArchive $zip): array
    {
        $stream = $zip->getStream('xl/sharedStrings.xml');
        if ($stream === false) {
            return [];
        }

        $tempPath = tempnam(sys_get_temp_dir(), 'xlsx_ss_r_');
        file_put_contents($tempPath, stream_get_contents($stream));
        fclose($stream);

        $reader = new XMLReader();
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

    private function isEmptyRow(array $row): bool
    {
        foreach ($row as $val) {
            if (trim((string) $val) !== '') {
                return false;
            }
        }
        return true;
    }

    private function normalizeString(string $val): string
    {
        $val = trim($val);
        if (!mb_check_encoding($val, 'UTF-8')) {
            $val = mb_convert_encoding($val, 'UTF-8', 'ISO-8859-1');
        }
        return $val;
    }
}
