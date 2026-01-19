"use client";

import type { ExtractedTable } from "@/types/api";
import { parseTables } from "@/types/api";

interface TablesViewerProps {
  tablesJson?: string;
  tableCount?: number;
}

export function TablesViewer({ tablesJson, tableCount }: TablesViewerProps) {
  const tables = parseTables(tablesJson);

  if (tables.length === 0) {
    return (
      <div className="text-center py-8 text-gray-500">
        No tables found in this document.
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="text-sm text-gray-500">
        {tableCount || tables.length} table{(tableCount || tables.length) !== 1 ? "s" : ""} found
      </div>

      {tables.map((table, tableIndex) => (
        <TableCard key={tableIndex} table={table} index={tableIndex} />
      ))}
    </div>
  );
}

function TableCard({ table, index }: { table: ExtractedTable; index: number }) {
  if (!table.rows || table.rows.length === 0) {
    return null;
  }

  return (
    <div className="bg-white rounded-lg border border-gray-200 overflow-hidden">
      <div className="px-4 py-3 bg-gray-50 border-b border-gray-200">
        <h4 className="font-medium text-gray-700">
          {table.name || `Table ${index + 1}`}
        </h4>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <tbody>
            {table.rows.map((row, rowIndex) => (
              <tr
                key={rowIndex}
                className={rowIndex % 2 === 0 ? "bg-white" : "bg-gray-50"}
              >
                {row.map((cell, cellIndex) => (
                  <td
                    key={cellIndex}
                    className="px-4 py-2 border-b border-gray-100 text-gray-700"
                  >
                    {cell || ""}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
