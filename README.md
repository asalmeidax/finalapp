# Brazuca Lite TV — fonte remota automática

Versão Android TV/Fire TV/Mi Stick que lê diretamente as duas bases de catálogo identificadas no add-on `plugin.video.BrazucaPlay.Matrix` 2.1.4.

## Atualização automática
Ao abrir o app:
1. a última lista salva aparece imediatamente;
2. o app consulta as bases remotas de filmes e séries;
3. calcula um SHA-256 do conteúdo;
4. se o hash mudou, substitui o cache local e atualiza a grade;
5. se a fonte estiver fora do ar, mantém a última lista salva.

Não há `config.json` para configurar.

## Fontes de catálogo usadas
- Filmes: `https://gist.githubusercontent.com/skyrisk/5b87797329c7b46422565ffbaab3be7e/raw/page.xml`
- Séries: `https://gist.githubusercontent.com/skyrisk/16070347f20c87c72540f9f805b57a66/raw/SeriesBase`

Esses endereços foram encontrados no código do add-on fornecido para análise.

## Reprodução
A versão lite reproduz diretamente HLS (`.m3u8`), MP4, DASH (`.mpd`), WebM e M4V via Android Media3. Itens cuja lista aponta para resolvedores específicos do Kodi ou páginas intermediárias continuam visíveis no catálogo, mas exibem um aviso em vez de tentar reproduzir.

## Gerar APK no GitHub
O workflow já está em `.github/workflows/build-apk.yml`.

1. envie o conteúdo desta pasta para a raiz do repositório;
2. abra **Actions**;
3. escolha **Build Android APK**;
4. **Run workflow**;
5. ao concluir, baixe o artifact **BrazucaLiteTV-BrazucaSource-debug**.

O APK fica dentro do artifact como `app-debug.apk`.
