import { FormEvent, KeyboardEvent, useEffect, useRef, useState } from 'react';
import axios from 'axios';
import api from '../api';
import { Icon } from '../components/Icon';
import { Avatar, StatusBadge, formatDate, formatSalary } from '../components/ui';
import { useAuth } from '../stores/AuthContext';
import type { AskResponse } from '../types';

const MAX_LENGTH = 300;

type ChatMessage =
  | { id: number; role: 'user'; text: string }
  | { id: number; role: 'assistant'; response: AskResponse }
  | { id: number; role: 'error'; text: string };

const baseExamples = [
  'Who joined in the last 3 months?',
  'How many people are in Engineering?',
  'Who is currently on leave?',
  'List everyone with "engineer" in their title',
];
const salaryExamples = ['Who are the 3 highest paid employees?'];

function AnswerBubble({ response }: { response: AskResponse }) {
  return (
    <div className="space-y-3">
      <div
        className={`rounded-2xl rounded-tl-sm px-4 py-3 text-sm ${
          response.answerable ? 'bg-white text-slate-800 ring-1 ring-slate-200' : 'bg-amber-50 text-amber-900 ring-1 ring-amber-200'
        }`}
      >
        {!response.answerable && <p className="mb-1 text-xs font-semibold uppercase tracking-wide text-amber-700">Can't answer that</p>}
        {response.answer}
      </div>

      {response.interpretedAs.length > 0 && (
        <div className="flex flex-wrap items-center gap-1.5">
          <span className="text-xs text-slate-500">Interpreted as</span>
          {response.interpretedAs.map((label) => (
            <span key={label} className="rounded-full bg-brand-50 px-2.5 py-0.5 text-xs font-medium text-brand-700 ring-1 ring-inset ring-brand-600/20">
              {label}
            </span>
          ))}
        </div>
      )}

      {response.employees.length > 0 && (
        <ul className="divide-y divide-slate-100 overflow-hidden rounded-xl bg-white ring-1 ring-slate-200">
          {response.employees.map((e) => (
            <li key={e.id} className="flex items-center gap-3 px-4 py-2.5">
              <Avatar firstName={e.firstName} lastName={e.lastName} size="sm" />
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-medium text-slate-900">{e.firstName} {e.lastName}</p>
                <p className="truncate text-xs text-slate-500">
                  {e.position}{e.departmentName ? ` · ${e.departmentName}` : ''} · Hired {formatDate(e.hireDate)}
                </p>
              </div>
              {e.salary != null && <span className="hidden text-sm tabular-nums text-slate-700 sm:block">{formatSalary(e.salary)}</span>}
              <StatusBadge status={e.status} />
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

function AskPage() {
  const { isAdmin, isManager } = useAuth();
  const examples = isAdmin || isManager ? [...baseExamples, ...salaryExamples] : baseExamples;

  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const nextId = useRef(0);
  const bottomRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' });
  }, [messages, loading]);

  const send = async (question: string) => {
    const text = question.trim();
    if (!text || loading) return;
    setMessages((m) => [...m, { id: nextId.current++, role: 'user', text }]);
    setInput('');
    setLoading(true);
    try {
      // Generous client timeout: the server already bounds the AI call and its retries
      const { data } = await api.post<AskResponse>('/ask', { question: text }, { timeout: 90_000 });
      setMessages((m) => [...m, { id: nextId.current++, role: 'assistant', response: data }]);
    } catch (err) {
      const message =
        (axios.isAxiosError(err) && err.response?.data?.message) ||
        (axios.isAxiosError(err) && err.code === 'ECONNABORTED' ? 'That took too long. Please try again.' : null) ||
        'Something went wrong. Please try again.';
      setMessages((m) => [...m, { id: nextId.current++, role: 'error', text: message }]);
    } finally {
      setLoading(false);
      inputRef.current?.focus();
    }
  };

  const onSubmit = (event: FormEvent) => {
    event.preventDefault();
    send(input);
  };

  // Enter sends, Shift+Enter adds a new line
  const onKeyDown = (event: KeyboardEvent<HTMLTextAreaElement>) => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      send(input);
    }
  };

  return (
    <div className="mx-auto flex h-[calc(100vh-8rem)] max-w-3xl flex-col lg:h-[calc(100vh-4rem)]">
      <div className="mb-4">
        <h1 className="text-2xl font-semibold text-slate-900 sm:text-3xl">Ask</h1>
        <p className="mt-1 text-sm text-slate-500">Ask questions about your team in plain English.</p>
      </div>

      <div className="card flex min-h-0 flex-1 flex-col overflow-hidden">
        <div className="flex-1 space-y-5 overflow-y-auto p-4 sm:p-6" aria-live="polite">
          {messages.length === 0 && (
            <div className="flex h-full flex-col items-center justify-center text-center">
              <div className="flex h-12 w-12 items-center justify-center rounded-full bg-brand-50 text-brand-600">
                <Icon name="sparkles" />
              </div>
              <h2 className="mt-4 font-semibold text-slate-900">What would you like to know?</h2>
              <p className="mt-1 max-w-md text-sm text-slate-500">
                AI turns your question into search filters, then EmployeeHub looks up the answer in your own data.
                Employee records are never sent to the AI.
              </p>
              <div className="mt-6 flex max-w-xl flex-wrap justify-center gap-2">
                {examples.map((example) => (
                  <button
                    key={example}
                    onClick={() => send(example)}
                    className="rounded-full border border-slate-200 bg-white px-3 py-1.5 text-sm text-slate-700 shadow-sm transition hover:border-brand-300 hover:bg-brand-50 hover:text-brand-700"
                  >
                    {example}
                  </button>
                ))}
              </div>
            </div>
          )}

          {messages.map((message) => {
            if (message.role === 'user') {
              return (
                <div key={message.id} className="flex justify-end">
                  <p className="max-w-[85%] whitespace-pre-wrap rounded-2xl rounded-tr-sm bg-brand-600 px-4 py-2.5 text-sm text-white">
                    {message.text}
                  </p>
                </div>
              );
            }
            return (
              <div key={message.id} className="flex gap-3">
                <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-brand-50 text-brand-600">
                  <Icon name="sparkles" className="h-4 w-4" />
                </span>
                <div className="min-w-0 flex-1">
                  {message.role === 'assistant' ? (
                    <AnswerBubble response={message.response} />
                  ) : (
                    <div className="flex items-start gap-2 rounded-2xl rounded-tl-sm bg-red-50 px-4 py-3 text-sm text-red-700 ring-1 ring-red-200">
                      <Icon name="alert" className="mt-0.5 h-4 w-4 shrink-0" />
                      {message.text}
                    </div>
                  )}
                </div>
              </div>
            );
          })}

          {loading && (
            <div className="flex gap-3">
              <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-brand-50 text-brand-600">
                <Icon name="sparkles" className="h-4 w-4" />
              </span>
              <div className="flex items-center gap-1.5 rounded-2xl rounded-tl-sm bg-white px-4 py-3 text-sm text-slate-500 ring-1 ring-slate-200">
                <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-slate-400 [animation-delay:-0.3s]" />
                <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-slate-400 [animation-delay:-0.15s]" />
                <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-slate-400" />
                <span className="ml-1">Thinking…</span>
              </div>
            </div>
          )}
          <div ref={bottomRef} />
        </div>

        <form onSubmit={onSubmit} className="border-t border-slate-100 bg-slate-50/60 p-3 sm:p-4">
          <div className="flex items-end gap-2">
            <div className="relative flex-1">
              <textarea
                ref={inputRef}
                value={input}
                onChange={(e) => setInput(e.target.value.slice(0, MAX_LENGTH))}
                onKeyDown={onKeyDown}
                rows={1}
                placeholder="Ask about your team…"
                className="input max-h-32 resize-none pr-14"
                aria-label="Your question"
                autoFocus
              />
              {input.length > MAX_LENGTH - 50 && (
                <span className="absolute bottom-2 right-3 text-xs text-slate-400">{input.length}/{MAX_LENGTH}</span>
              )}
            </div>
            <button type="submit" className="btn-primary h-[38px] px-3" disabled={loading || !input.trim()} aria-label="Send">
              <Icon name="send" className="h-4 w-4" />
            </button>
          </div>
          <p className="mt-2 text-xs text-slate-400">Enter to send · Shift+Enter for a new line</p>
        </form>
      </div>
    </div>
  );
}

export default AskPage;
