<div align="center">

# 🍔 Lançador NFC-e

Aplicação desktop para agilizar o lançamento de atendimentos em uma lancheria.

**Feito sob medida para resolver uma necessidade real — sem pagar por um sistema caro só por causa de uma funcionalidade.**

</div>

---

## 💡 Sobre o projeto

O Lançador NFC-e nasceu de uma necessidade bem prática: a lancheria da minha sogra precisava de uma funcionalidade específica, mas acabava pagando caro por um sistema completo. A ideia foi criar uma ferramenta simples, focada no fluxo que ela precisava.

O aplicativo permite preencher os dados de um lançamento, relacionar um cliente e imprimir uma **prévia não fiscal** em uma impressora térmica. Também oferece cadastro local de clientes e uma tela para selecionar um certificado digital A1.

> **Importante:** apesar do nome e da opção “Emitir”, a emissão e autorização de NFC-e ainda não estão implementadas. O botão “Emitir” apenas informa essa limitação; a impressão disponível é somente uma prévia não fiscal. Este projeto não substitui um sistema fiscal autorizado.

## ✨ Funcionalidades

- Formulário de lançamento com valor e tipo de item.
- Busca de clientes por nome ou CPF.
- Cadastro, edição e exclusão de clientes, armazenados localmente em SQLite.
- Impressão de prévia não fiscal em impressora térmica configurada no computador.
- Seleção e validação de certificado digital A1 (`.pfx` ou `.p12`). O aplicativo salva o caminho do arquivo, mas **não armazena a senha**.
- Interface desktop em JavaFX, com opção de tela cheia.

## 🧰 Tecnologias

- Java
- JavaFX
- Maven
- SQLite
- JUnit 5

## 🚀 Como executar

### Pré-requisitos

- JDK 17 ou superior.
- Git (opcional, para clonar o repositório).
- Uma impressora configurada no sistema operacional, caso queira testar a impressão da prévia.

### Executar com Maven Wrapper

Clone o repositório e entre na pasta do projeto:

```bash
git clone https://github.com/Matheeus00/lancador-nfce.git
cd lancador-nfce
```

No Linux ou macOS:

```bash
./mvnw clean javafx:run
```

No Windows:

```bat
mvnw.cmd clean javafx:run
```

O Maven Wrapper baixa as dependências necessárias na primeira execução. Também é possível executar os testes com `./mvnw test` (Linux/macOS) ou `mvnw.cmd test` (Windows).

## 🗺️ Próximos passos

- Implementar a integração com um provedor fiscal para emissão e autorização de NFC-e.
- Completar e validar o fluxo de configuração fiscal para uso em ambiente real.

Até que essas etapas sejam concluídas, use o projeto apenas para cadastro de clientes e impressão de prévias não fiscais.

## 📄 Licença

Nenhuma licença foi definida neste repositório até o momento. Consulte o autor antes de reutilizar, modificar ou distribuir o projeto.

---

<div align="center">

Projeto pessoal, criado para resolver um problema de verdade. ❤️

</div>
