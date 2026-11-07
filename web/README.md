# Консоль стоп-факторов

Админ-консоль стоп-факторов на SvelteKit 3 (Svelte 5). Дизайн-система, UI-компоненты (Ark UI,
Remix Icon), тема и конфигурация взяты из консоли [vard](https://github.com/vard-systems/vard)
(MIT).

## Запуск

```sh
bun install
bun run dev          # http://localhost:5173
```

Страницы рендерит сервер SvelteKit (`adapter-node`), он же проксирует `/api/v1/admin` в бэкенд
стоп-факторов (`src/hooks.server.ts`, `src/lib/server/api.ts`). Адрес бэкенда — `API_URL`
(по умолчанию `http://localhost:9090`, `src/env.ts`).

Пользователь из меню в шапке хранится в cookie; сервер передаёт его бэкенду заголовком `X-User`
(автор версии). Каждое изменение стоп-фактора, правила или условия спрашивает причину — она
сохраняется в истории версий.

Production: `bun run build`, затем `ORIGIN=https://console.example API_URL=http://backend:9090 node build`.

## Структура

- `src/lib/components/ui` — компоненты vard: кнопки, поля, таблицы, диалоги, вкладки, уведомления
- `src/lib/components/layout` — оболочка: шапка, боковое меню, меню пользователя
- `src/lib/stop-factors` — логика стоп-факторов без UI: подписи, условия, форма правила, поля и
  запросы, фильтр списка, изменение с причиной (`*.spec.ts` — тесты)
- `src/lib/components/stop-factors`, `metrics` — правила, условия, настройки, история, метрики
- `src/lib/api` — типы DTO, клиент, функции эндпоинтов core-console
- `src/lib/theme/theme.ts` — цвета, размеры, шрифты; `bun run theme` генерирует
  `src/lib/styles/tokens.css` и `fonts.css` (не редактировать вручную)

## Проверки

```sh
bun run check && bun run lint && bun run test && bun run build
```
