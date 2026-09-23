-- Sigma JE / JEE Command Center
-- Run this once in the Supabase SQL editor.
-- Catalog tables are shared; student progress is protected per authenticated user.

create extension if not exists pgcrypto;

create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  display_name text,
  target_exam text not null default 'JEE 2027',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.subjects (
  id text primary key,
  name text not null unique,
  sort_order int not null default 0
);

create table if not exists public.chapters (
  id text primary key,
  subject_id text not null references public.subjects(id) on delete cascade,
  number int not null,
  name text not null,
  difficulty int not null default 3 check (difficulty between 1 and 5),
  estimated_minutes int not null default 240,
  unique(subject_id, number)
);

create table if not exists public.topics (
  id text primary key,
  chapter_id text not null references public.chapters(id) on delete cascade,
  sort_order int not null default 0,
  title text not null,
  unique(chapter_id, sort_order)
);

create table if not exists public.questions (
  id text primary key,
  subject_id text not null references public.subjects(id) on delete cascade,
  chapter_id text not null references public.chapters(id) on delete cascade,
  topic_id text references public.topics(id) on delete set null,
  chapter_name text not null,
  topic text not null,
  prompt text not null,
  options jsonb not null,
  correct_index int not null,
  explanation text not null default '',
  difficulty int not null default 3 check (difficulty between 1 and 5),
  source text not null default 'Sigma JE',
  year int,
  marks int not null default 4,
  negative_marks numeric not null default 1
);

create table if not exists public.chapter_progress (
  user_id uuid not null references auth.users(id) on delete cascade,
  chapter_id text not null references public.chapters(id) on delete cascade,
  progress real not null default 0 check (progress between 0 and 1),
  confidence int not null default 0 check (confidence between 0 and 5),
  last_studied_at timestamptz,
  primary key(user_id, chapter_id)
);

create table if not exists public.topic_progress (
  user_id uuid not null references auth.users(id) on delete cascade,
  topic_id text not null references public.topics(id) on delete cascade,
  completed boolean not null default false,
  primary key(user_id, topic_id)
);

create table if not exists public.question_attempts (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  question_id text not null references public.questions(id) on delete cascade,
  chapter_id text not null references public.chapters(id) on delete cascade,
  topic_id text references public.topics(id) on delete set null,
  selected_index int not null,
  correct boolean not null,
  response_time_sec int not null default 0,
  created_at timestamptz not null default now()
);

create index if not exists question_attempts_user_created_idx on public.question_attempts(user_id, created_at desc);
create index if not exists question_attempts_user_chapter_idx on public.question_attempts(user_id, chapter_id);
create index if not exists questions_chapter_idx on public.questions(chapter_id);
create index if not exists topics_chapter_idx on public.topics(chapter_id);

alter table public.profiles enable row level security;
alter table public.subjects enable row level security;
alter table public.chapters enable row level security;
alter table public.topics enable row level security;
alter table public.questions enable row level security;
alter table public.chapter_progress enable row level security;
alter table public.topic_progress enable row level security;
alter table public.question_attempts enable row level security;

drop policy if exists "catalog subjects readable" on public.subjects;
create policy "catalog subjects readable" on public.subjects for select to authenticated using (true);
drop policy if exists "catalog chapters readable" on public.chapters;
create policy "catalog chapters readable" on public.chapters for select to authenticated using (true);
drop policy if exists "catalog topics readable" on public.topics;
create policy "catalog topics readable" on public.topics for select to authenticated using (true);
drop policy if exists "catalog questions readable" on public.questions;
create policy "catalog questions readable" on public.questions for select to authenticated using (true);

drop policy if exists "own profile read" on public.profiles;
create policy "own profile read" on public.profiles for select to authenticated using (id = auth.uid());
drop policy if exists "own profile insert" on public.profiles;
create policy "own profile insert" on public.profiles for insert to authenticated with check (id = auth.uid());
drop policy if exists "own profile update" on public.profiles;
create policy "own profile update" on public.profiles for update to authenticated using (id = auth.uid()) with check (id = auth.uid());

drop policy if exists "own chapter progress" on public.chapter_progress;
create policy "own chapter progress" on public.chapter_progress for all to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());
drop policy if exists "own topic progress" on public.topic_progress;
create policy "own topic progress" on public.topic_progress for all to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());
drop policy if exists "own attempts" on public.question_attempts;
create policy "own attempts" on public.question_attempts for all to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());

-- Initial catalog roots. Chapters/topics/questions can be seeded from the app's
-- canonical catalog/question bank after running this schema.
insert into public.subjects(id, name, sort_order) values
  ('physics', 'Physics', 1),
  ('chemistry', 'Chemistry', 2),
  ('mathematics', 'Mathematics', 3)
on conflict (id) do update set name = excluded.name, sort_order = excluded.sort_order;
