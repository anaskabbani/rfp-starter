"use client";

import { useState } from "react";
import type { KeyValuePair } from "@/types/api";
import { parseKeyValues } from "@/types/api";

interface KeyValuesViewerProps {
  keyValuesJson?: string;
}

export function KeyValuesViewer({ keyValuesJson }: KeyValuesViewerProps) {
  const keyValues = parseKeyValues(keyValuesJson);
  const [copiedIndex, setCopiedIndex] = useState<number | null>(null);

  const handleCopy = async (value: string, index: number) => {
    await navigator.clipboard.writeText(value);
    setCopiedIndex(index);
    setTimeout(() => setCopiedIndex(null), 2000);
  };

  if (keyValues.length === 0) {
    return (
      <div className="text-center py-8 text-gray-500">
        No key-value pairs extracted from this document.
      </div>
    );
  }

  return (
    <div>
      <div className="text-sm text-gray-500 mb-4">
        {keyValues.length} field{keyValues.length !== 1 ? "s" : ""} extracted
      </div>

      <div className="bg-white rounded-lg border border-gray-200 divide-y divide-gray-100">
        {keyValues.map((kv, index) => (
          <KeyValueRow
            key={index}
            keyValue={kv}
            index={index}
            isCopied={copiedIndex === index}
            onCopy={handleCopy}
          />
        ))}
      </div>
    </div>
  );
}

interface KeyValueRowProps {
  keyValue: KeyValuePair;
  index: number;
  isCopied: boolean;
  onCopy: (value: string, index: number) => void;
}

function KeyValueRow({ keyValue, index, isCopied, onCopy }: KeyValueRowProps) {
  return (
    <div className="flex items-start gap-4 p-4 hover:bg-gray-50 transition-colors group">
      <div className="w-1/3 flex-shrink-0">
        <span className="text-sm font-medium text-gray-600">
          {keyValue.key}
        </span>
      </div>
      <div className="flex-1 flex items-start gap-2">
        <span className="text-sm text-gray-900 flex-1">
          {keyValue.value}
        </span>
        <button
          onClick={() => onCopy(keyValue.value, index)}
          className="opacity-0 group-hover:opacity-100 transition-opacity p-1 rounded hover:bg-gray-200"
          title="Copy value"
        >
          {isCopied ? (
            <svg className="h-4 w-4 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
            </svg>
          ) : (
            <svg className="h-4 w-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z"
              />
            </svg>
          )}
        </button>
      </div>
    </div>
  );
}
