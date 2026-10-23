# SHDIA ULTRA v2 — Android + IA na nuvem

Esta é uma **base de projeto funcional para desenvolvimento**, não um APK pronto. Ela inclui:
- aplicativo Android nativo em Kotlin + Jetpack Compose;
- API FastAPI para receber trabalhos de melhoria;
- arquitetura cloud-first: os recursos avançados exigem internet;
- seletor de conteúdo (Anime / Live Action / Restauração);
- fluxo de upload e consulta de status preparado para integrar modelos reais.

## O que funciona nesta base
- API `/health` para verificar o servidor.
- API `POST /jobs` que recebe uma imagem, valida tipo/tamanho e cria um trabalho demonstrativo.
- API `GET /jobs/{job_id}` para consultar status.
- App Android com interface inicial, escolha de estilo, seleção de imagem e envio para API.
- Tela mostra o estado do envio/processamento.

## O que ainda NÃO está integrado
- Modelo de IA real para super-resolução/restauração.
- Processamento de vídeos e exportação final de vídeo.
- Autenticação, armazenamento permanente, fila de tarefas e cobrança/limites de uso.
- APK compilado/assinado.

O backend retorna `demo_mode`, porque não seria correto fingir que a IA já processou a imagem. Para produção, substitua o trecho demonstrativo em `backend/main.py` por um worker que execute modelos reais (por exemplo, modelos de super-resolução/restauração apropriados à licença e hardware disponíveis).

## Requisitos
- Android Studio recente + SDK Android.
- JDK 17.
- Python 3.10+.

## Rodar o backend localmente
```bash
cd backend
python -m venv .venv
# Windows: .venv\Scripts\activate
# macOS/Linux:
source .venv/bin/activate
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8000
```
Teste `http://127.0.0.1:8000/health`.

Para o celular acessar o computador na mesma rede, configure no app o endereço IP local do computador, por exemplo `http://192.168.1.20:8000`. Para uso real pela internet, publique a API em um servidor com HTTPS. **Não use HTTP sem TLS em produção.**

## Abrir o app
Abra a pasta `android/` no Android Studio. O app está configurado para apontar para `http://10.0.2.2:8000` no emulador Android. Para dispositivo físico, altere `BASE_URL` em `MainActivity.kt` para o endereço do servidor acessível pelo celular.

## Próximas etapas para produção
1. Integrar modelo real de IA e worker assíncrono.
2. Adicionar upload em armazenamento temporário privado e apagar arquivos automaticamente.
3. Adicionar processamento de vídeo em segmentos, limites de duração/tamanho e estimativa de custo.
4. Configurar HTTPS, autenticação, rate limiting, logs sem dados sensíveis e política de privacidade.
5. Testar em aparelhos reais e gerar APK assinado.
