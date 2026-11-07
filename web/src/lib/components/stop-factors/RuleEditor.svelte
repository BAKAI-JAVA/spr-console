<script lang="ts">
	import { untrack } from 'svelte';
	import { messagesOf, type Calculator, type Metric, type Rule } from '#lib/api/index.js';
	import {
		Button,
		FieldGrid,
		FormSection,
		FullscreenDialog,
		Input,
		Note,
		ProblemList,
		Select,
		SwitchField
	} from '#lib/components/ui/index.js';
	import { hasValueTo, RESULT_ORDER } from '#lib/stop-factors/conditions.js';
	import {
		OPERATOR_HINTS,
		RESULT_EFFECTS,
		RESULTS,
		VALUE_TYPES
	} from '#lib/stop-factors/labels.js';
	import { fromRuleForm, toRuleForm, type RuleForm } from '#lib/stop-factors/rule-form.js';

	type Props = {
		open: boolean;
		metric: Metric;
		calculator: Calculator | undefined;
		/** The rule being edited; none for a new rule. */
		rule?: Rule | null;
		title: string;
		submitLabel?: string;
		/** Saves the rule: a refusal it throws is shown in the sheet; false keeps the sheet open. */
		onSave: (rule: Rule) => Promise<boolean | void> | boolean | void;
	};

	let {
		open = $bindable(false),
		metric,
		calculator,
		rule = null,
		title,
		submitLabel = 'Сохранить правило',
		onSave
	}: Props = $props();

	const context = $derived({ metric, calculator });
	const list = $derived(calculator?.valueKind === 'LIST');
	const specs = $derived(new Map((calculator?.params ?? []).map((p) => [p.key, p])));

	const valueTypeOptions = $derived(
		(calculator?.allowedConditionValueTypes ?? []).map((type) => ({
			value: type,
			label: VALUE_TYPES[type]
		}))
	);
	const operatorOptions = $derived(
		(calculator?.allowedOperators ?? []).map(({ code, name }) => ({
			value: code,
			label: name,
			description: OPERATOR_HINTS[code]
		}))
	);

	let form = $state<RuleForm>(
		toRuleForm(
			null,
			untrack(() => context)
		)
	);
	let saving = $state(false);
	let problems = $state<string[]>([]);

	// The sheet starts from the rule each time it opens.
	$effect.pre(() => {
		if (open) {
			form = toRuleForm(rule, context);
			problems = [];
		}
	});

	async function save() {
		saving = true;
		problems = [];
		try {
			if ((await onSave(fromRuleForm(form, rule, context))) !== false) open = false;
		} catch (err) {
			problems = messagesOf(err);
		} finally {
			saving = false;
		}
	}
</script>

<FullscreenDialog bind:open {title} meta={metric.title}>
	<FormSection
		title="Правило"
		description="Правило проверяется отдельно от остальных правил стоп-фактора: свои параметры метрики и свои условия."
	>
		<Input
			label="Название"
			bind:value={form.name}
			maxlength={255}
			placeholder={metric.nameTemplate}
			hint="Пустое — по шаблону метрики из параметров."
		/>

		<SwitchField
			label="Правило проверяется"
			description="Отключённое правило сохраняется в стоп-факторе, но не участвует в решении."
			bind:checked={form.active}
		/>
	</FormSection>

	{#if metric.params?.length}
		<FormSection title="Параметры метрики" description="С какими значениями считается метрика.">
			<FieldGrid>
				{#each metric.params as param (param.key)}
					{@const spec = specs.get(param.key)}
					{#if spec?.allowedValues?.length}
						<Select
							label={param.label}
							bind:value={form.params[param.key]}
							options={spec.allowedValues.map((value) => ({ value }))}
							required={param.required}
							clearable={!param.required}
						/>
					{:else}
						<Input
							label={param.label}
							bind:value={form.params[param.key]}
							required={param.required}
							inputmode={spec?.type === 'INTEGER' ? 'numeric' : undefined}
							hint={param.required ? undefined : 'Необязательный'}
						/>
					{/if}
				{/each}
			</FieldGrid>
		</FormSection>
	{/if}

	<FormSection
		title="Условия"
		description="x — значение метрики заявки. Отказ проверяется первым; андеррайтер — жёлтая зона."
	>
		{#each RESULT_ORDER as result (result)}
			{@const condition = form.conditions[result]}
			<div class="condition">
				<SwitchField
					label={RESULTS[result]}
					description={RESULT_EFFECTS[result]}
					bind:checked={condition.enabled}
				/>

				{#if condition.enabled}
					{@const ranged = hasValueTo(condition.operator)}
					<FieldGrid columns={ranged ? 2 : 3}>
						<Select
							label="Тип значения"
							bind:value={condition.valueType}
							options={valueTypeOptions}
						/>
						<Select label="Оператор" bind:value={condition.operator} options={operatorOptions} />
						<Input
							label={ranged ? 'Значение от' : 'Значение'}
							bind:value={condition.value}
							required
							hint={list ? 'Статусы кредита через запятую' : undefined}
						/>
						{#if ranged}
							<Input label="Значение до" bind:value={condition.valueTo} required />
						{/if}
					</FieldGrid>
					<Note>{OPERATOR_HINTS[condition.operator]}</Note>
				{/if}
			</div>
		{/each}
	</FormSection>

	<ProblemList {problems} />

	{#snippet actions()}
		<Button variant="subtle" onclick={() => (open = false)} disabled={saving}>Отмена</Button>
		<Button onclick={save} loading={saving} disabled={saving}>
			{saving ? 'Сохранение…' : submitLabel}
		</Button>
	{/snippet}
</FullscreenDialog>

<style>
	.condition {
		display: flex;
		flex-direction: column;
		gap: var(--space-3);
	}

	.condition + .condition {
		padding-top: var(--space-4);
		border-top: 1px solid var(--color-border);
	}
</style>
