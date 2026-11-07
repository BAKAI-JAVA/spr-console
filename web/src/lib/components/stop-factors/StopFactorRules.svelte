<script lang="ts">
	import { refreshAll } from '$app/navigation';
	import {
		rulesApi,
		type Calculator,
		type Metric,
		type Rule,
		type StopFactor
	} from '#lib/api/index.js';
	import { Button, FormSection, IconButton, notify } from '#lib/components/ui/index.js';
	import { askReason } from '#lib/state/reason.svelte.js';
	import { versionedChange } from '#lib/stop-factors/change.js';
	import { isActiveRule, toRequestRule } from '#lib/stop-factors/rules.js';
	import RuleEditor from './RuleEditor.svelte';
	import RuleList from './RuleList.svelte';

	type Props = { stopFactor: StopFactor; metric: Metric; calculator: Calculator | undefined };

	let { stopFactor, metric, calculator }: Props = $props();

	/** The rule in the sheet; null for a new rule. */
	let editing = $state<Rule | null>(null);
	let sheetOpen = $state(false);
	let busy = $state(false);

	const factorId = $derived(stopFactor.factorId);
	const onlyRule = $derived(stopFactor.rules.length === 1);

	function openRule(rule: Rule | null) {
		editing = rule;
		sheetOpen = true;
	}

	/** Saves the sheet: a refusal is thrown back for the sheet to show beside the fields. */
	async function saveRule(next: Rule): Promise<boolean> {
		const reason = await askReason(editing ? 'Изменить правило' : 'Добавить правило');
		if (!reason) return false;

		if (editing?.id) await rulesApi.update(factorId, editing.id, next, reason);
		else await rulesApi.add(factorId, next, reason);
		await refreshAll();
		notify.success(editing ? 'Правило сохранено' : 'Правило добавлено');
		return true;
	}

	async function run(change: Parameters<typeof versionedChange>[0]) {
		busy = true;
		try {
			await versionedChange(change);
		} finally {
			busy = false;
		}
	}

	function toggle(rule: Rule) {
		const active = !isActiveRule(rule);
		void run({
			title: active ? 'Включить правило' : 'Отключить правило',
			run: (reason) =>
				rulesApi.update(factorId, rule.id!, { ...toRequestRule(rule), name: null, active }, reason),
			success: active ? 'Правило включено' : 'Правило отключено',
			failure: 'Не удалось изменить правило'
		});
	}

	function remove(rule: Rule) {
		void run({
			title: `Удалить правило «${rule.name}»`,
			danger: true,
			run: (reason) => rulesApi.delete(factorId, rule.id!, reason),
			success: 'Правило удалено',
			failure: 'Не удалось удалить правило'
		});
	}
</script>

<FormSection
	title="Правила"
	description="Каждое правило считает метрику со своими параметрами. Любое изменение создаёт новую версию стоп-фактора."
	icon="ri-list-check-3"
>
	{#snippet action()}
		<Button
			variant="subtle"
			size="sm"
			icon="ri-add-line"
			onclick={() => openRule(null)}
			disabled={busy}
		>
			Добавить правило
		</Button>
	{/snippet}

	<RuleList rules={stopFactor.rules} {metric}>
		{#snippet actions(rule)}
			<IconButton
				icon="ri-pencil-line"
				label="Изменить"
				size="sm"
				disabled={busy}
				onclick={() => openRule(rule)}
			/>
			<IconButton
				icon={isActiveRule(rule) ? 'ri-pause-circle-line' : 'ri-play-circle-line'}
				label={isActiveRule(rule) ? 'Отключить' : 'Включить'}
				size="sm"
				disabled={busy}
				onclick={() => toggle(rule)}
			/>
			<IconButton
				icon="ri-delete-bin-line"
				label={onlyRule ? 'Единственное правило: отключите или удалите стоп-фактор' : 'Удалить'}
				size="sm"
				colorPalette="danger"
				disabled={busy || onlyRule}
				onclick={() => remove(rule)}
			/>
		{/snippet}
	</RuleList>
</FormSection>

<RuleEditor
	bind:open={sheetOpen}
	{metric}
	{calculator}
	rule={editing}
	title={editing ? (editing.name ?? 'Правило') : 'Новое правило'}
	submitLabel={editing ? 'Сохранить правило' : 'Добавить правило'}
	onSave={saveRule}
/>
