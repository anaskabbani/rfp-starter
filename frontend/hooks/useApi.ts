"use client";

import { useAuth } from "@clerk/nextjs";
import { useMemo } from "react";
import { createApiClient, createApi, Api } from "@/lib/api";

export function useApi(): Api {
  const { getToken } = useAuth();

  const api = useMemo(() => {
    const client = createApiClient(getToken);
    return createApi(client);
  }, [getToken]);

  return api;
}
