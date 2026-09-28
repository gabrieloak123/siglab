import { useCallback, useState } from "react";

export interface Feedback {
  loading: boolean;
  error: string;
  success: string;
  setLoading: (loading: boolean) => void;
  clear: () => void;
  begin: () => void;
  fail: (message: string) => void;
  succeed: (message: string) => void;
}

export const useFeedback = (): Feedback => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const clear = useCallback(() => {
    setError("");
    setSuccess("");
  }, []);

  const begin = useCallback(() => {
    setLoading(true);
    setError("");
    setSuccess("");
  }, []);

  return {
    loading,
    error,
    success,
    setLoading,
    clear,
    begin,
    fail: setError,
    succeed: setSuccess,
  };
};
