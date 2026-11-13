# Galeria One UI

Uma galeria alternativa leve e moderna para Android, baseada na linguagem visual **Samsung One UI**, focada em fluidez a 60–120 fps, gestos naturais e organização local de fotos e vídeos sem recursos invasivos de IA.

## O que ela oferece

- **Interface One UI responsiva**: cabeçalho expansível que recolhe ao rolar (alcance facilmente com uma mão), barra flutuante em cápsula (`Fotos` e `Álbuns`) e suporte a tema claro/escuro adaptado ao sistema.
- **Grade de fotos em formato quadrado com pinça fluida**: miniaturas quadradas de ponta a ponta com gesto de pinça para alternar continuamente entre **Dias**, **Semanas**, **Meses** e **Anos**, com reposicionamento espacial estilo FLIP das fotos visíveis.
- **Visualizador interativo**:
  - Zoom com pinça e duplo toque até `4x` + pan suave.
  - Arraste vertical para baixo com transição direta e proporcional de volta para a grade de fotos.
  - Arraste vertical para cima (com sensibilidade calibrada para evitar abertura acidental no zoom 1x) para exibir a ficha de detalhes e metadados EXIF.
  - Navegação lateral entre mídias e suporte integrado a vídeos.
- **Editor de fotos integrado**:
  - **Recorte interativo (`Recortar`)**: moldura recortável com arraste livre ou em proporções fixas (`Livre`, `Original`, `1:1`, `4:5`, `4:3`, `16:9`, `9:16`), alças nos cantos e bordas, grade dos terços, rotação fina (`-45°` a `+45°`), giros de `±90°` e espelhamento horizontal/vertical.
  - **Filtros e Ajustes**: filtros visuais rápidos, brilho, contraste e saturação com pré-visualização em tempo real e salvamento diretamente via `MediaStore`.
  - **Desenho livre**: traços com seleção de cor e espessura diretamente sobre a imagem.
- **Álbuns, Colagens e Álbum Privado**:
  - Acesso direto aos álbuns do dispositivo com cartões de destaque (Favoritos, Vídeos, Recentes e Lixeira) e contador no topo.
  - Criador de colagens multi-layout e Álbum Privado protegido por PIN.
- **APK enxuto e otimizado**: compilado com **R8**, shrink de recursos, Baseline Profile (`ProfileInstaller`) e cache de miniaturas decodificadas sob medida para evitar engasgos (*stuttering*) na rolagem.

## Tecnologias e ferramentas utilizadas

| Camada | Ferramentas |
| :--- | :--- |
| **Linguagem & UI** | Kotlin, Jetpack Compose, Material 3 (tokens e componentes customizados One UI) |
| **Animações & Gestos** | Compose `Animatable` (spring physics), `SharedBounds`/FLIP spatial interpolation, `PointerInput` multi-pass |
| **Mídia & Imagens** | Android `MediaStore` API, Coil (`coil-compose`, `coil-video`), `ExifInterface`, `Media3` / `VideoView` |
| **Processamento Gráfico** | Android `Bitmap`, `Canvas`, `Matrix`, `ColorMatrix` (recorte, rotação e filtros em hardware/native) |
| **Armazenamento & Privacidade** | `SharedPreferences` / estado local seguro para PIN do Álbum Privado, Lixeira local com restauração |
| **Build & Otimização** | Gradle Kotlin DSL, Android Gradle Plugin, **R8** (minificação e resource shrinking), `ProfileInstaller` |

## Instalação

Baixe o APK mais recente na página de [Releases](https://github.com/guimark1412/samsung-oneui-gallery/releases) (`SamsungOneUIGallery.apk`) ou compile localmente:

```powershell
.\gradlew.bat assembleRelease
```

O APK de release será gerado em `app/build/outputs/apk/release/app-release.apk`.
