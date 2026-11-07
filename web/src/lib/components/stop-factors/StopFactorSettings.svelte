<script lang="ts">
	import { goto, refreshAll } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { messagesOf, stopFactorsApi, type Product, type StopFactor } from '#lib/api/index.js';
	import { Button, DangerZone, ProblemList, notify } from '#lib/components/ui/index.js';
	import { askReason } from '#lib/state/reason.svelte.js';
	import { fieldsChanged, fieldsOf, updateRequest, type Fields } from '#lib/stop-factors/fields.js';
	import StopFactorFields from './StopFactorFields.svelte';

	type Props = { stopFactor: StopFactor; products: Product[] };

	let { stopFactor, products }: Props = $props();

	// The form follows the stop factor: a new version, saved here or elsewhere, refills it.
	let fields = $derived<Fields>(fieldsOf(stopFactor));
	let saving = $state(false);
	let problems = $state<string[]>([]);

	const dirty = $derived(fieldsChanged(fields, stopFactor));

	async function save() {
		const reason = await askReason('Сохранить настройки стоп-фактора');
		if (!reason) return;

		saving = true;
		problems = [];
		try {
			await stopFactorsApi.update(stopFactor.factorId, updateRequest(stopFactor, fields, reason));
			await refreshAll();
			notify.success('Настройки сохранены');
		} catch (err) {
			problems = messagesOf(err);
		} finally {
			saving = false;
		}
	}

	async function remove() {
		const reason = await askReason(`Удалить стоп-фактор «${stopFactor.name}»?`, {
			description:
				'Стоп-фактор перестанет проверять заявки и пропадёт из списка. История версий и проверки по ним сохранятся.',
			confirmLabel: 'Удалить',
			danger: true
		});
		if (!reason) return;

		try {
			await stopFactorsApi.delete(stopFactor.factorId, reason);
			notify.success('Стоп-фактор удалён', stopFactor.name);
			await goto(resolve('/(panel)/stop-factors'));
		} catch (err) {
			notify.error(err, 'Не удалось удалить стоп-фактор');
		}
	}
</script>

<div class="settings">
	<StopFactorFields bind:fields {products} namePlaceholder={stopFactor.name} />

	<ProblemList {problems} />

	<div class="actions">
		<Button
			variant="subtle"
			onclick={() => (fields = fieldsOf(stopFactor))}
			disabled={!dirty || saving}
		>
			Отменить
		</Button>
		<Button onclick={save} loading={saving} disabled={!dirty || saving}>
			{saving ? 'Сохранение…' : 'Сохранить изменения'}
		</Button>
	</div>

	<DangerZone
		title="Удалить стоп-фактор"
		description="Он перестанет проверять заявки. Версии и проверки по ним сохранятся; чтобы временно выключить проверку, отключите стоп-фактор."
		label="Удалить"
		onclick={remove}
	/>
</div>

<style>
	.settings {
		display: flex;
		flex-direction: column;
		gap: var(--space-4);
	}

	.actions {
		display: flex;
		justify-content: flex-end;
		gap: var(--space-2);
	}
</style>
