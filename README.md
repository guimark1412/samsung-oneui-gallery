# Galeria One UI

Uma galeria alternativa leve e moderna para Android, baseada na linguagem visual **Samsung One UI**, projetada para entregar fluidez a 60–120 fps, gestos naturais, edição completa de imagens e organização local de mídias sem recursos invasivos ou pesados de IA.

> **Adendo:** *Galeria feita especialmente e unicamente para **Manuela Almeida**.*

---

## Sobre o projeto

Este aplicativo nasceu como uma alternativa completa à galeria padrão do Android — trazendo a ergonomia de uso com uma mão, cabeçalho expansível e estética da **Samsung One UI** com foco em desempenho mesmo em aparelhos intermediários e de entrada (como o **Redmi 9 / MIUI**, incluindo `largeHeap`, compatibilidade com armazenamento legado no Android 10–12 e assinatura oficial `v1 + v2 + v3` unificada).

Todo o código roda 100% localmente, sem permissão de internet (`INTERNET`) e sem rastreadores, compilado com **R8** (preservando os símbolos legíveis com `-dontobfuscate` para maior compatibilidade com Play Protect e verificadores MIUI) e **Baseline Profiles (`ProfileInstaller`)** para carregamento imediato.

---

## O que ela oferece

- **Interface One UI responsiva**: cabeçalho expansível que recolhe ao rolar (alcance com uma mão em qualquer aba), barra flutuante em cápsula (`Fotos` e `Álbuns`) e alternância rápida entre tema claro e escuro.
- **Grade de fotos quadrada com pinça fluida**: miniaturas quadradas `1:1` de ponta a ponta com gesto de pinça para alternar continuamente entre **Dias**, **Semanas**, **Meses** e **Anos**, com reposicionamento espacial estilo FLIP das fotos visíveis.
- **Visualizador interativo**:
  - Zoom com pinça e duplo toque até `4x` + pan suave.
  - Arraste vertical para baixo com transição direta e proporcional de volta para a grade de fotos.
  - Arraste vertical para cima (com sensibilidade calibrada para evitar abertura acidental no zoom 1x) para exibir detalhes e metadados EXIF.
  - Navegação lateral entre mídias e suporte integrado a vídeos.
- **Editor de fotos integrado**:
  - **Recorte interativo (`Recortar`)**: moldura recortável com arraste livre ou em proporções fixas (`Livre`, `Original`, `1:1`, `4:5`, `4:3`, `16:9`, `9:16`), alças nos cantos e bordas, grade dos terços, rotação fina (`-45°` a `+45°`), giros de `±90°` e espelhamento.
  - **Filtros e Ajustes**: filtros visuais rápidos, brilho, contraste, saturação, temperatura e vinheta com pré-visualização em tempo real e salvamento via `MediaStore`.
  - **Desenho livre e Texto**: traços e textos com seleção de cor diretamente sobre a imagem.
- **Álbuns, Colagens e Álbum Privado**:
  - Cartões de acesso rápido (Favoritos, Vídeos, Recentes e Lixeira), criador de colagens multi-layout e **Álbum Privado** protegido por PIN.

---

## Tecnologias e ferramentas utilizadas

| Camada | Ferramentas |
| :--- | :--- |
| **Linguagem & UI** | Kotlin, Jetpack Compose, Material 3 (tokens e componentes customizados One UI) |
| **Animações & Gestos** | Compose `Animatable` (spring physics), transição FLIP / Shared Bounds, `PointerInput` multi-pass |
| **Mídia & Imagens** | Android `MediaStore` API, Coil (`coil-compose`, `coil-video`), `ExifInterface`, `VideoView` |
| **Processamento Gráfico** | Android `Bitmap`, `Canvas`, `Matrix`, `ColorMatrix` (recorte, rotação, vinheta e filtros) |
| **Armazenamento & Privacidade** | `SharedPreferences` local para PIN do Álbum Privado, Lixeira local com restauração, zero acesso à internet |
| **Build, Assinatura & Otimização** | Gradle Kotlin DSL, Android Gradle Plugin, **R8** (`-dontobfuscate` + resource shrinking), `ProfileInstaller`, Keystore PKCS12 (`v1 + v2 + v3`) |

---

## Instalação

Baixe o APK mais recente na página de [Releases](https://github.com/guimark1412/samsung-oneui-gallery/releases) (`SamsungOneUIGallery.apk`) ou compile localmente:

```powershell
.\gradlew.bat assembleRelease
```

O APK de release assinado será gerado em `app/build/outputs/apk/release/app-release.apk`.
