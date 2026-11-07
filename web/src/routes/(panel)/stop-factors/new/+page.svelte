<script lang="ts">
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { messagesOf, stopFactorsApi, type Rule } from '#lib/api/index.js';
	import { pageTitle } from '#lib/brand.js';
	import {
		Button,
		EmptyState,
		FormSection,
		IconButton,
		Input,
		LinkButton,
		Note,
		PageHeader,
		ProblemList,
		Select,
		notify
	} from '#lib/components/ui/index.js';
	import RuleEditor from '#lib/components/stop-factors/RuleEditor.svelte';
	import RuleList from '#lib/components/stop-factors/RuleList.svelte';
	import StopFactorFields from '#lib/components/stop-factors/StopFactorFields.svelte';
	import { emptyFields, requestFields } from '#lib/stop-factors/fields.js';
	import type { PageProps } from './$types';

	let { data }: PageProps = $props();

	let metricId = $derived(data.metricId);
	let fields = $state(emptyFields());
	let rules = $state<Rule[]>([]);
	let changeReason = $state('');

	/** The rule in the sheet: its index, rules.length for a new one. */
	let editing = $state(0);
	let sheetOpen = $state(false);

	let saving = $state(false);
	let problems = $state<string[]>([]);

	const metric = $derived(data.metrics.find((m) => String(m.id) === metricId));
	const calculator = $derived(data.calculators.find((c) => c.type === metric?.calculator));
	const metricOptions = $derived(
		data.metrics.map((m) => ({
			value: String(m.id),
			label: m.title,
			description: data.calculators.find((c) => c.type === m.calculator)?.name
		}))
	);

	function chooseMetric(value: string) {
		if (value === metricId) return;
		metricId = value;
		rules = [];
		if (value) openRule(0);
	}

	function openRule(index: number) {
		editing = index;
		sheetOpen = true;
	}

	function saveRule(rule: Rule) {
		rules[editing] = rule;
	}

	async function create() {
		if (!metric?.id) return;
		saving = true;
		problems = [];
		try {
			const created = await stopFactorsApi.create({
				metricId: metric.id,
				...requestFields(fields),
				rules,
				changeReason: changeReason.trim() || null
			});
			notify.success('Стоп-фактор создан', created.name);
			await goto(
				resolve('/(panel)/stop-factors/[factorId]', { factorId: String(created.factorId) })
			);
		} catch (err) {
			problems = messagesOf(err);
		} finally {
			saving = false;
		}
	}
</script>

<svelte:head><title>{pageTitle('Новый стоп-фактор')}</title></svelte:head>

<PageHeader
	crumbs={[
		'Решения',
		{ label: 'Стоп-факторы', href: resolve('/(panel)/stop-factors') },
		'Новый стоп-фактор'
	]}
	description="Стоп-фактор — это метрика и одно или несколько правил: у каждого правила свои параметры метрики и свои пороги отказа и андеррайтера."
/>

<div class="form">
	<FormSection
		title="Метрика"
		description="Что измеряет стоп-фактор. Калькулятор метрики задаёт параметры правил и доступные операторы."
		icon="ri-function-line"
	>
		<Select
			label="Метрика"
			value={metricId}
			options={metricOptions}
			placeholder="Выберите метрику"
			onChange={chooseMetric}
			required
		/>
		{#if metric}
			<Note boxed>
				Шаблон названия правила: <code>{metric.nameTemplate}</code>
			</Note>
		{/if}
	</FormSection>

	{#if metric}
		<FormSection
			title="Правила"
			description="Каждое правило проверяется отдельно. Решение стоп-фактора — самое строгое из решений его правил."
			icon="ri-list-check-3"
		>
			{#snippet action()}
				<Button
					variant="subtle"
					size="sm"
					icon="ri-add-line"
					onclick={() => openRule(rules.length)}
				>
					Добавить правило
				</Button>
			{/snippet}

			{#if rules.length}
				<RuleList {rules} {metric}>
					{#snippet actions(rule)}
						<IconButton
							icon="ri-pencil-line"
							label="Изменить"
							size="sm"
							onclick={() => openRule(rules.indexOf(rule))}
						/>
						<IconButton
							icon="ri-delete-bin-line"
							label="Убрать"
							size="sm"
							colorPalette="danger"
							onclick={() => rules.splice(rules.indexOf(rule), 1)}
						/>
					{/snippet}
				</RuleList>
			{:else}
				<EmptyState icon="ri-list-check-3" title="Правил пока нет">
					Добавьте хотя бы одно правило: параметры метрики и условия отказа или андеррайтера.
				</EmptyState>
			{/if}
		</FormSection>

		<StopFactorFields bind:fields products={data.products} />

		<FormSection title="История" description="Причина создания сохраняется в первой версии.">
			<Input
				label="Причина создания"
				bind:value={changeReason}
				maxlength={255}
				placeholder="Создание стоп-фактора"
			/>
		</FormSection>

		<ProblemList {problems} />

		<div class="actions">
			<LinkButton href={resolve('/(panel)/stop-factors')} variant="subtle">Отмена</LinkButton>
			<Button
				onclick={create}
				loading={saving}
				disabled={saving || rules.length === 0}
				icon="ri-check-line"
			>
				{saving ? 'Создание…' : 'Создать стоп-фактор'}
			</Button>
		</div>

		<RuleEditor
			bind:open={sheetOpen}
			{metric}
			{calculator}
			rule={rules[editing] ?? null}
			title={editing < rules.length ? `Правило ${editing + 1}` : 'Новое правило'}
			submitLabel={editing < rules.length ? 'Сохранить правило' : 'Добавить правило'}
			onSave={saveRule}
		/>
	{/if}
</div>

<style>
	.form {
		display: flex;
		flex-direction: column;
		gap: var(--space-4);
		padding-bottom: var(--space-6);
	}

	.actions {
		display: flex;
		justify-content: flex-end;
		gap: var(--space-2);
	}

	code {
		font-family: var(--font-mono);
		font-size: var(--text-sm);
	}
</style>
