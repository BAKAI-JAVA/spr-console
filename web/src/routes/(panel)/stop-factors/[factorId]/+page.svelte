<script lang="ts">
	import { resolve } from '$app/paths';
	import { stopFactorsApi } from '#lib/api/index.js';
	import { pageTitle } from '#lib/brand.js';
	import { Badge, Button, PageHeader, Tabs, type Tab } from '#lib/components/ui/index.js';
	import StopFactorHistory from '#lib/components/stop-factors/StopFactorHistory.svelte';
	import StopFactorRules from '#lib/components/stop-factors/StopFactorRules.svelte';
	import StopFactorSettings from '#lib/components/stop-factors/StopFactorSettings.svelte';
	import { versionedChange } from '#lib/stop-factors/change.js';
	import { segmentLabel, statusLabel } from '#lib/stop-factors/labels.js';
	import { formatDateTime } from '#lib/utils/format.js';
	import { setSearchParams } from '#lib/utils/url.js';
	import type { PageProps } from './$types';

	let { data }: PageProps = $props();

	const sf = $derived(data.stopFactor);
	const tab = $derived(data.tab);

	const tabs = $derived<Tab[]>([
		{ value: 'rules', label: 'Правила', icon: 'ri-list-check-3', count: sf.rules.length },
		{ value: 'settings', label: 'Настройки', icon: 'ri-settings-3-line' },
		{ value: 'history', label: 'История', icon: 'ri-history-line', count: data.versions.length }
	]);

	let switching = $state(false);

	function show(next: string) {
		void setSearchParams({ tab: next === 'rules' ? undefined : next });
	}

	async function toggleStatus() {
		const active = !sf.active;
		switching = true;
		try {
			await versionedChange({
				title: active ? 'Включить стоп-фактор' : 'Отключить стоп-фактор',
				description: active
					? 'Стоп-фактор снова начнёт проверять заявки.'
					: 'Стоп-фактор перестанет проверять заявки; его правила сохранятся.',
				confirmLabel: active ? 'Включить' : 'Отключить',
				run: (reason) => stopFactorsApi.changeStatus(sf.factorId, active, reason),
				success: active ? 'Стоп-фактор включён' : 'Стоп-фактор отключён',
				failure: 'Не удалось изменить статус'
			});
		} finally {
			switching = false;
		}
	}
</script>

<svelte:head><title>{pageTitle(sf.name)}</title></svelte:head>

<div class="page">
	<div class="heading">
		<PageHeader
			crumbs={[
				'Решения',
				{ label: 'Стоп-факторы', href: resolve('/(panel)/stop-factors') },
				sf.name
			]}
		>
			{#snippet meta()}
				<Badge tone={sf.active ? 'success' : 'neutral'}>{statusLabel(sf.active)}</Badge>
				<Badge>{segmentLabel(sf.segment)}</Badge>
				<Badge>v{sf.versionNumber}</Badge>
			{/snippet}

			{#snippet actions()}
				<Button
					variant="subtle"
					icon={sf.active ? 'ri-pause-circle-line' : 'ri-play-circle-line'}
					onclick={toggleStatus}
					loading={switching}
					disabled={switching}
				>
					{sf.active ? 'Отключить' : 'Включить'}
				</Button>
			{/snippet}
		</PageHeader>

		<p class="summary">
			<span><i class="ri-function-line" aria-hidden="true"></i>{sf.metricTitle}</span>
			<span class="mono">#{sf.factorId}</span>
			<span>
				<i class="ri-time-line" aria-hidden="true"></i>
				{formatDateTime(sf.validFrom)}{sf.createdBy ? ` · ${sf.createdBy}` : ''}
			</span>
		</p>

		{#if sf.description}
			<p class="description">{sf.description}</p>
		{/if}
	</div>

	<Tabs {tabs} bind:value={() => tab, show} label="Разделы стоп-фактора">
		{#snippet panel(value)}
			<div class="panel">
				{#if value === 'rules'}
					<StopFactorRules stopFactor={sf} metric={data.metric} calculator={data.calculator} />
				{:else if value === 'settings'}
					<StopFactorSettings stopFactor={sf} products={data.products} />
				{:else if value === 'history'}
					<StopFactorHistory versions={data.versions} metric={data.metric} />
				{/if}
			</div>
		{/snippet}
	</Tabs>
</div>

<style>
	.page {
		display: flex;
		flex-direction: column;
		gap: var(--space-4);
	}

	.heading {
		display: flex;
		flex-direction: column;
		gap: var(--space-1);
	}

	.summary {
		display: flex;
		flex-wrap: wrap;
		gap: var(--space-1) var(--space-4);
		margin: 0;
		color: var(--color-text-hint);
		font-size: var(--text-sm);
	}

	.summary i {
		margin-right: 4px;
		vertical-align: -2px;
	}

	.mono {
		font-family: var(--font-mono);
	}

	.description {
		margin: var(--space-1) 0 0;
		max-width: 75ch;
		color: var(--color-text-hint);
		font-size: var(--text-base);
	}

	.panel {
		padding-bottom: var(--space-6);
	}
</style>
