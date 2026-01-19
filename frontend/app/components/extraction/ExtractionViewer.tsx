"use client";

import type { RfpDocumentExtraction } from "@/types/api";
import { Tabs, TabList, TabTrigger, TabContent } from "../ui/Tabs";
import { ExtractionStats } from "./ExtractionStats";
import { TextViewer } from "./TextViewer";
import { TablesViewer } from "./TablesViewer";
import { KeyValuesViewer } from "./KeyValuesViewer";
import { parseTables, parseKeyValues } from "@/types/api";

interface ExtractionViewerProps {
  extraction: RfpDocumentExtraction;
}

export function ExtractionViewer({ extraction }: ExtractionViewerProps) {
  const tables = parseTables(extraction.tablesJson);
  const keyValues = parseKeyValues(extraction.keyValuesJson);

  // Determine the best default tab
  const getDefaultTab = () => {
    if (extraction.status === "FAILED") return "overview";
    if (keyValues.length > 0) return "key-values";
    if (extraction.extractedText) return "text";
    if (tables.length > 0) return "tables";
    return "overview";
  };

  return (
    <Tabs defaultValue={getDefaultTab()}>
      <TabList>
        <TabTrigger value="overview">Overview</TabTrigger>
        <TabTrigger value="text">
          Extracted Text
          {extraction.characterCount && (
            <span className="ml-2 text-xs text-gray-400">
              ({(extraction.characterCount / 1000).toFixed(1)}k)
            </span>
          )}
        </TabTrigger>
        <TabTrigger value="tables">
          Tables
          {tables.length > 0 && (
            <span className="ml-2 text-xs bg-gray-100 text-gray-600 px-1.5 py-0.5 rounded-full">
              {tables.length}
            </span>
          )}
        </TabTrigger>
        <TabTrigger value="key-values">
          Key-Values
          {keyValues.length > 0 && (
            <span className="ml-2 text-xs bg-gray-100 text-gray-600 px-1.5 py-0.5 rounded-full">
              {keyValues.length}
            </span>
          )}
        </TabTrigger>
      </TabList>

      <TabContent value="overview">
        <ExtractionStats extraction={extraction} />
      </TabContent>

      <TabContent value="text">
        <TextViewer
          text={extraction.extractedText || ""}
          characterCount={extraction.characterCount}
        />
      </TabContent>

      <TabContent value="tables">
        <TablesViewer
          tablesJson={extraction.tablesJson}
          tableCount={extraction.tableCount}
        />
      </TabContent>

      <TabContent value="key-values">
        <KeyValuesViewer keyValuesJson={extraction.keyValuesJson} />
      </TabContent>
    </Tabs>
  );
}
