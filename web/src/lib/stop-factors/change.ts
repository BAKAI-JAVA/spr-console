import { refreshAll } from '$app/navigation';
import { notify } from '#lib/components/ui/index.js';
import { askReason } from '#lib/state/reason.svelte.js';

type VersionedChange = {
	/** The question the reason dialog asks: "Отключить правило". */
	title: string;
	description?: string;
	confirmLabel?: string;
	danger?: boolean;
	/** Makes the change with the reason the administrator gave. */
	run: (reason: string) => Promise<unknown>;
	/** The notification once it is saved. */
	success: string;
	/** What a failure is called when the API sends no sentence of its own. */
	failure: string;
};

/**
 * A change that creates a new version of a stop factor: asks why, makes it, reloads the page
 * (the new version has new rule ids) and says so. Resolves to whether it was saved; a failure is
 * shown as a notification.
 */
export async function versionedChange(change: VersionedChange): Promise<boolean> {
	const reason = await askReason(change.title, {
		description: change.description,
		confirmLabel: change.confirmLabel ?? (change.danger ? 'Удалить' : 'Сохранить'),
		danger: change.danger
	});
	if (!reason) return false;

	try {
		await change.run(reason);
		await refreshAll();
		notify.success(change.success);
		return true;
	} catch (err) {
		notify.error(err, change.failure);
		return false;
	}
}
