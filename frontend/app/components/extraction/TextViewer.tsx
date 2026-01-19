"use client";

import { useState } from "react";
import { Button } from "../ui/Button";

interface TextViewerProps {
  text: string;
  characterCount?: number;
}

export function TextViewer({ text, characterCount }: TextViewerProps) {
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    await navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  if (!text) {
    return (
      <div className="text-center py-8 text-gray-500">
        No text extracted from this document.
      </div>
    );
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <div className="text-sm text-gray-500">
          {characterCount?.toLocaleString() || text.length.toLocaleString()} characters
        </div>
        <Button
          variant="secondary"
          size="sm"
          onClick={handleCopy}
        >
          {copied ? (
            <>
              <svg className="h-4 w-4 mr-1.5 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
              </svg>
              Copied!
            </>
          ) : (
            <>
              <svg className="h-4 w-4 mr-1.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z"
                />
              </svg>
              Copy All
            </>
          )}
        </Button>
      </div>

      <div className="bg-gray-50 rounded-lg border border-gray-200 p-4 max-h-[600px] overflow-auto custom-scrollbar">
        <pre className="text-sm text-gray-700 whitespace-pre-wrap font-mono leading-relaxed">
          {text}
        </pre>
      </div>
    </div>
  );
}
