import { fetchEventSource } from "@microsoft/fetch-event-source";
import type {
    PerguntaRequest,
    PerguntaResponse,
    RespostaMessage,
    StatusConexao,
} from '../types/conversa'

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';
const ENDPOINT_PERGUNTAS = import.meta.env.VITE_ENDPOINT_PERGUNTAS ?? '/perguntas';
const ENDPOINT_SSE = import.meta.env.VITE_ENDPOINT_SSE ?? '/respostas/stream';
const ENDPOINT_LOGIN = import.meta.env.VITE_ENDPOINT_LOGIN ?? '/auth/login';

export const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true';

let mockListener: ((msg: RespostaMessage) => void) | null = null;

function criarHeaders(token: string | null, extra?: Record<string, string>): Record<string, string> {
    const headers: Record<string, string> = { ...extra };
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }
    return headers;
}

// Autenticação (JWT)
export async function login(usuario: string, senha: string): Promise<string> {
    if (USE_MOCK) {
        await new Promise((resolve) => setTimeout(resolve, 300));
        return `mock-jwt-${usuario}`;
    }
    const response = await fetch(`${BASE_URL}${ENDPOINT_LOGIN}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json'},
        body: JSON.stringify({ username: usuario, password: senha}),
    });
    if (!response.ok){
        throw new Error(`Falha no login (HTTP ${response.status})`);
    }
    const data = (await response.json()) as {
        token?: string;
        acessToken?: string;
        jwt?: string;
    };
    const token = data.token ?? data.acessToken ?? data.jwt;
    if (!token) {
        throw new Error('Gateway não retornou um token JWT válido.')
    }

    return token;
}