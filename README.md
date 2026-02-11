# SUS - Microserviço de Notificações

## Configuração

### Variáveis de Ambiente

Este projeto utiliza variáveis de ambiente para configuração. Crie um arquivo `.env` na raiz do projeto baseado no arquivo `.env.example`:

### Variáveis Necessárias

#### Database
- `DB_USERNAME`: Usuário do banco de dados PostgreSQL
- `DB_PASSWORD`: Senha do banco de dados PostgreSQL

#### RabbitMQ
- `RABBITMQ_HOST`: Host do RabbitMQ (localhost para desenvolvimento)
- `RABBITMQ_PORT`: Porta do RabbitMQ (padrão: 5672)
- `RABBITMQ_USERNAME`: Usuário do RabbitMQ
- `RABBITMQ_PASSWORD`: Senha do RabbitMQ

#### Email (Gmail)
- `MAIL_USERNAME`: Seu email do Gmail
- `MAIL_PASSWORD`: Senha de aplicativo do Gmail (não use sua senha pessoal)
  - Para gerar uma senha de aplicativo: https://myaccount.google.com/apppasswords

#### Twilio (SMS)
- `TWILIO_ACCOUNT_SID`: Account SID do Twilio
- `TWILIO_AUTH_TOKEN`: Auth Token do Twilio
- `TWILIO_PHONE_NUMBER`: Número de telefone do Twilio (formato: +1234567890)

## Executando o Projeto

### 1. Estrutura de Diretórios

Clone os 3 microserviços dentro de um diretório principal chamado `sustech`:

```
sustech/                              ← Diretório principal
├── sus-microsservico-core/          ← Microserviço Core (contém o docker-compose.yml)
├── sus-microsservico-agendamento/   ← Microserviço de Agendamento
└── sus-microservico-notificacoes/   ← Microserviço de Notificações
```

### 2. Configuração

Configure o arquivo `.env` de cada microserviço baseado no arquivo `.env.example` presente em cada projeto.

### 3. Build e Execução

Abra o terminal no diretório `sus-microsservico-core` e execute:

```bash
docker-compose build --no-cache
```