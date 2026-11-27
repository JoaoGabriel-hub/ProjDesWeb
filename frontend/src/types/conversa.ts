export interface PerguntaRequest {
    pergunta: string;
}

export interface PerguntaResponse {
    correlationId: string;
}

export interface RespostaMessage {
    correlationId: string;
    userId: string;
    resposta: string | null;
    sucesso: boolean;
    erro: string | null;
    respondidaEm: string;
}

// Estados possíveis das perguntas 
export type StatusInteracao = 'aguardando' | 'respondida' | 'erro'; 

export interface InteracaoConversa {
    correlationId: string;
    pergunta: string;
    status: StatusInteracao;
    resposta?: string;
    erro?: string;
    criadoEm: string;
    respondidaEm?: string;
}

// Estado da conexão SSE (Cosmético)
export type StatusConexao = 'desconectado' | 'conectando' | 'conectado' | 'erro';