# Galeria One UI (Samsung One UI Gallery Clone)

Aplicativo Android de Galeria e Editor de Fotos construído em **Kotlin + Jetpack Compose**, desenhado visualmente seguindo as diretrizes oficiais da **Samsung One UI** (One UI 6/7) para rodar de forma leve e fluida no **Moto G15** e no emulador **BlueStacks**.

## Recursos Principais

- **Design Samsung One UI (Ergonomia + Squircle):**
  - Cabeçalho expansível e recolhível (`OneUICollapsingScaffold`) com área de visualização ampla no topo e barra de ações ao alcance do polegar.
  - Cards agrupados com cantos arredondados estilo *squircle* (`26.dp` e `18.dp`).
  - Alternância instantânea entre **Modo Escuro AMOLED** e **Modo Claro One UI**.
- **Aba Imagens:**
  - Grade agrupada por data (*Hoje*, *Ontem*, datas anteriores).
  - **Pinch-to-zoom** dinâmico e botões de zoom para alternar entre **2 e 6 colunas**.
  - Pesquisa em tempo real por nome de arquivo ou álbum.
  - Seleção múltipla com barra flutuante (*Enviar*, *Favoritar*, *Lixeira*).
- **Aba Álbuns:**
  - Filtros rápidos (*Todos*, *Câmera*, *Favoritos*, *Vídeos*) e cards de álbuns com contagem de itens.
- **Aba Histórias:**
  - Coleções automáticas (*Destaques Recentes*, *Favoritos da Galeria*, *Coleção de Viagem & Arte*, *Todos os Momentos*) com **Player de Histórias em tela cheia** e barra de progresso animada.
- **Aba Menu:**
  - Acesso rápido a **Favoritos**, **Vídeos**, **Recentes**, **Lixeira** (com restauração e exclusão permanente), alternância de tema e gerador de **12 fotos artísticas de demonstração** (ideal para testar em emuladores vazios).
- **Visualizador em Tela Cheia (`ViewerScreen`) & Detalhes:**
  - Navegação por deslize (`HorizontalPager`), zoom por pinça e duplo toque.
  - Barra superior e inferior translúcidas no estilo Samsung e **Bottom Sheet de Detalhes (EXIF)** com resolução, megapixels, tamanho e álbum.
- **Editor de Fotos Integrado (`EditorScreen`):**
  - **Transformar:** Rotação ±90°, espelhamento horizontal/vertical e recorte por proporção (*Original*, *1:1*, *4:3*, *16:9*).
  - **Filtros:** 9 filtros em tempo real (*Original, Vivo, Quente, Frio, P&B, Noir, Sépia, Retrô, Cyberpunk*).
  - **Ajustes:** Brilho, Contraste, Saturação, Temperatura e Vinheta.
  - **Desenhar & Texto:** Desenho livre com seleção de cores, espessura, desfazer e adição de textos.
  - Salva uma cópia em alta resolução diretamente na galeria (`Pictures/Galeria Editadas`).

## Tecnologias Utilizadas

- **Kotlin** + **Jetpack Compose** (Material 3 customizado com tokens One UI)
- **Android MediaStore API** (imagens e vídeos)
- **Coil** (`coil-compose` & `coil-video`)
- **Canvas & ColorMatrix** para preview e exportação de edições

## Como Compilar

Requisitos: **JDK 17** e **Android SDK (API 34)**.

```bash
./gradlew assembleDebug
```

O APK será gerado em `app/build/outputs/apk/debug/app-debug.apk`.
