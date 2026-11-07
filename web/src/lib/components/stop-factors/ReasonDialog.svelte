<script lang="ts">
	import { ConfirmDialog, Textarea } from '#lib/components/ui/index.js';
	import { reasonRequest } from '#lib/state/reason.svelte.js';

	let reason = $state('');
	let error = $state('');

	const request = $derived(reasonRequest.current);

	function setOpen(open: boolean) {
		if (open) return;
		reasonRequest.current?.resolve(null);
	}

	function confirm() {
		const text = reason.trim();
		if (!text) {
			error = 'Укажите причину: она сохраняется в истории версий';
			return;
		}
		request?.resolve(text);
	}

	$effect.pre(() => {
		if (request) {
			reason = '';
			error = '';
		}
	});
</script>

<ConfirmDialog
	bind:open={() => request !== null, setOpen}
	title={request?.title ?? ''}
	description={request?.description ??
		'Изменение создаёт новую версию стоп-фактора; прежняя версия и проверки по ней сохраняются.'}
	tone={request?.danger ? 'danger' : 'info'}
	icon={request?.danger ? 'ri-delete-bin-line' : 'ri-git-commit-line'}
	confirmLabel={request?.confirmLabel ?? 'Сохранить'}
	onConfirm={confirm}
>
	<Textarea
		label="Причина изменения"
		bind:value={reason}
		{error}
		rows={3}
		placeholder="Например: новые пороги по решению кредитного комитета"
		required
	/>
</ConfirmDialog>
