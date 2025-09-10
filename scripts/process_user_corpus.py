#!/usr/bin/env python3

def process_corpus_file(input_file, output_file="dictionary_470k.txt"):
    """Processa arquivo com 470k palavras acentuadas"""
    
    mojibake_map = {
        'á': '?', 'à': '?', 'â': '?', 'ã': '?', 'ä': '?',
        'é': '?', 'è': '?', 'ê': '?', 'ë': '?',
        'í': '?', 'ì': '?', 'î': '?', 'ï': '?',
        'ó': '?', 'ò': '?', 'ô': '?', 'õ': '?', 'ö': '?',
        'ú': '?', 'ù': '?', 'û': '?', 'ü': '?',
        'ç': '??', 'ñ': '?'
    }
    
    processed = 0
    
    with open(input_file, 'r', encoding='utf-8') as f_in:
        with open(output_file, 'w', encoding='utf-8') as f_out:
            
            for line in f_in:
                word = line.strip().lower()
                
                if not word or len(word) < 3:
                    continue
                
                # Gera padrão mojibake
                broken = word
                for correct, broken_char in mojibake_map.items():
                    broken = broken.replace(correct, broken_char)
                
                if broken != word:
                    f_out.write(f"{word}|{broken}\n")
                    processed += 1
    
    print(f"Processadas {processed} palavras com mojibake")
    return processed

if __name__ == "__main__":
    # Processa seu arquivo
    count = process_corpus_file("palavras_com_especiais.txt")
    print(f"Dicionário criado com {count} entradas!")