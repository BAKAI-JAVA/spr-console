<script lang="ts">
	import type { Metric, StopFactor } from '#lib/api/index.js';
	import {
		Badge,
		CellText,
		DataTable,
		FormSection,
		FullscreenDialog,
		Note,
		type Column
	} from '#lib/components/ui/index.js';
	import { segmentLabel, statusLabel } from '#lib/stop-factors/labels.js';
	import { countOf, formatDateTime } from '#lib/utils/format.js';
	import RuleList from './RuleList.svelte';

	type Props = { versions: StopFactor[]; metric: Metric };

	let { versions, metric }: Props = $props();

	const columns: Column[] = [
		{ key: 'version', label: 'Версия', min: '6rem' },
		{ key: 'reason', label: 'Причина', min: '18rem', grow: true },
		{ key: 'author', label: 'Автор', min: '9rem' },
		{ key: 'period', label: 'Действовала', min: '16rem' },
		{ key: 'status', label: 'Статус', min: '8rem' }
	];

	const rows = $derived(versions.map((version) => ({ id: String(version.versionId), version })));

	let opened = $state<StopFactor | null>(null);
	let open = $state(false);

	function show(version: StopFactor) {
		opened = version;
		open = true;
	}
</script>

<DataTable
	{columns}
	{rows}
	empty="Версий нет."
	onOpen={(row) => show(row.version)}
	label={(row) => `Открыть версию ${row.version.versionNumber}`}
>
	{#snippet row({ version })}
		<td>
			<span class="version">
				<span class="mono">v{version.versionNumber}</span>
				{#if version.current}<Badge tone="success">Текущая</Badge>{/if}
			</span>
		</td>
		<td>
			<CellText
				title={version.changeReason ?? '—'}
				detail={countOf(version.rules.length, 'правило', 'правила', 'правил')}
			/>
		</td>
		<td><span class="muted">{version.createdBy ?? '—'}</span></td>
		<td>
			<span class="muted">
				{formatDateTime(version.validFrom)} — {version.validTo
					? formatDateTime(version.validTo)
					: 'сейчас'}
			</span>
		</td>
		<td>
			<Badge tone={version.active ? 'success' : 'neutral'}>
				{statusLabel(version.active)}
			</Badge>
		</td>
	{/snippet}
</DataTable>

{#if opened}
	<FullscreenDialog
		bind:open
		title="Версия {opened.versionNumber}"
		meta={opened.name}
		description={opened.changeReason ?? undefined}
	>
		<FormSection title="Версия" description="Так стоп-фактор проверял заявки в этот период.">
			<dl>
				<dt>Название</dt>
				<dd>{opened.name}</dd>
				<dt>Действовала</dt>
				<dd>
					{formatDateTime(opened.validFrom)} — {opened.validTo
						? formatDateTime(opened.validTo)
						: 'сейчас'}
				</dd>
				<dt>Автор</dt>
				<dd>{opened.createdBy ?? '—'}</dd>
				<dt>Статус</dt>
				<dd>{statusLabel(opened.active)}</dd>
				<dt>Сегмент</dt>
				<dd>{segmentLabel(opened.segment)}</dd>
				<dt>Продукты</dt>
				<dd>{opened.productIds.length ? opened.productIds.join(', ') : 'Все продукты'}</dd>
			</dl>
			{#if !opened.current}
				<Note>Это прежняя версия: изменить её нельзя, проверки по ней сохраняются.</Note>
			{/if}
		</FormSection>

		<FormSection title="Правила" description="Правила и условия этой версии.">
			<RuleList rules={opened.rules} {metric} />
		</FormSection>
	</FullscreenDialog>
{/if}

<style>
	.version {
		display: inline-flex;
		align-items: center;
		gap: var(--space-2);
	}

	.mono {
		font-family: var(--font-mono);
		font-size: var(--text-sm);
	}

	.muted {
		color: var(--color-text-hint);
		font-size: var(--text-sm);
	}

	dl {
		display: grid;
		grid-template-columns: max-content 1fr;
		gap: var(--space-2) var(--space-5);
		margin: 0;
	}

	dt {
		color: var(--color-text-hint);
	}

	dd {
		margin: 0;
	}
</style>
