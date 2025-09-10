# Demojibakelizador Enterprise Desktop

Aplicação desktop standalone para correção de encoding de arquivos.

## Arquitetura

- **Core**: Engine Go compilado como biblioteca nativa (DLL/SO)
- **GUI**: JavaFX com JNA para FFI
- **Build**: Maven + JPackage para distribuição

## Build Rápido

```bash
./build.sh
```

## Estrutura

```
demojibake/
├── core/                    # Engine Go
│   ├── demojibake.go       # FFI exports
│   └── build.sh            # Cross-compile script
├── gui/                     # JavaFX app
│   ├── src/main/java/
│   │   ├── core/           # JNA bindings
│   │   └── ui/             # Interface
│   └── pom.xml             # Maven config
└── build.sh                # Main build
```

## Features

- ✅ Performance nativa (Go + JavaFX)
- ✅ Interface moderna
- ✅ Processamento paralelo
- ✅ Cross-platform (Windows/macOS/Linux)
- ✅ Empacotamento com JPackage

## Requisitos

- Go 1.21+
- Java 21+
- Maven 3.9+