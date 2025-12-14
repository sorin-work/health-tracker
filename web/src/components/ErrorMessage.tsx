import { ApiError } from '../api/client';

export function errorText(error: unknown): string {
  if (error instanceof ApiError) return error.message;
  if (error instanceof Error) return error.message;
  return 'Something went wrong';
}

export function ErrorMessage({ error }: { error: unknown }) {
  if (!error) return null;
  return (
    <div className="alert alert--error" role="alert">
      {errorText(error)}
    </div>
  );
}
