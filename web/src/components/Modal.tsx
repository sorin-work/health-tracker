import { useEffect, useId, useRef, type ReactNode } from 'react';
import { CloseIcon } from './icons';

export function Modal({
  title,
  onClose,
  children,
}: {
  title: string;
  onClose: () => void;
  children: ReactNode;
}) {
  const titleId = useId();
  const dialogRef = useRef<HTMLDivElement>(null);
  // Parents pass a new arrow function on every render. Keep the latest one in a ref so the effect
  // below runs once; re-running it moved focus back to the first field while the user typed.
  const onCloseRef = useRef(onClose);
  useEffect(() => {
    onCloseRef.current = onClose;
  }, [onClose]);

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onCloseRef.current();
    };
    document.addEventListener('keydown', onKey);
    // Start in the first field of the form rather than on the close button in the header.
    const dialog = dialogRef.current;
    (
      dialog?.querySelector<HTMLElement>('input:not([disabled]), select:not([disabled]), textarea') ??
      dialog?.querySelector<HTMLElement>('button')
    )?.focus();
    return () => document.removeEventListener('keydown', onKey);
  }, []);

  return (
    <div className="modal-backdrop" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal" role="dialog" aria-modal="true" aria-labelledby={titleId} ref={dialogRef}>
        <div className="modal__header">
          <h2 id={titleId}>{title}</h2>
          <button className="icon-button" aria-label="Close" onClick={onClose}>
            <CloseIcon />
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}
