// The "why is this changed" question every versioned change asks: the reason is kept in the
// stop factor's version history. One dialog for the panel (ReasonDialog in its layout).

type ReasonRequest = {
	title: string;
	description?: string;
	confirmLabel: string;
	danger: boolean;
	resolve: (reason: string | null) => void;
};

export const reasonRequest = $state<{ current: ReasonRequest | null }>({ current: null });

/** Resolves to the reason, or to null when the dialog is cancelled. */
export function askReason(
	title: string,
	options: { description?: string; confirmLabel?: string; danger?: boolean } = {}
): Promise<string | null> {
	reasonRequest.current?.resolve(null);

	return new Promise((resolve) => {
		let settled = false;
		const request: ReasonRequest = {
			title,
			description: options.description,
			confirmLabel: options.confirmLabel ?? 'Сохранить',
			danger: options.danger ?? false,
			// The dialog closing after a confirm calls this again: only the first call counts.
			resolve: (reason) => {
				if (settled) return;
				settled = true;
				if (reasonRequest.current?.resolve === request.resolve) reasonRequest.current = null;
				resolve(reason);
			}
		};
		reasonRequest.current = request;
	});
}
