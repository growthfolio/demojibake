#!/usr/bin/env python3

import re
from collections import Counter

def extract_portuguese_words(text_file, min_freq=10):
    """Extrai palavras portuguesas com acentos de um corpus"""
    
    pt_pattern = re.compile(r'\b[a-záàâãéêíóôõúç]+\b', re.IGNORECASE)
    word_freq = Counter()
    
    with open(text_file, 'r', encoding='utf-8') as f:
        for line in f:
            words = pt_pattern.findall(line.lower())
            accented_words = [w for w in words if has_accents(w)]
            word_freq.update(accented_words)
    
    return {word: freq for word, freq in word_freq.items() if freq >= min_freq}

def has_accents(word):
    """Verifica se palavra tem acentos portugueses"""
    accents = 'áàâãéêíóôõúç'
    return any(c in accents for c in word)

def generate_mojibake_patterns(word):
    """Gera padrões de mojibake para uma palavra"""
    
    mojibake_map = {
        'á': '?', 'à': '?', 'â': '?', 'ã': '?',
        'é': '?', 'ê': '?', 'í': '?', 'ó': '?', 
        'ô': '?', 'õ': '?', 'ú': '?', 'ç': '??'
    }
    
    broken = word
    for correct, broken_char in mojibake_map.items():
        broken = broken.replace(correct, broken_char)
    
    return [broken] if broken != word else []

if __name__ == "__main__":
    # Exemplo com corpus básico
    sample_text = """
    A aplicação de correção automática é uma função importante.
    Não podemos ignorar a configuração adequada do sistema.
    São necessárias operações específicas para cada situação.
    O coração da solução está na informação precisa.
    """
    
    with open("sample_corpus.txt", "w", encoding="utf-8") as f:
        f.write(sample_text * 100)  # Simula frequência
    
    words = extract_portuguese_words("sample_corpus.txt", min_freq=5)
    
    with open("dictionary_expanded.txt", "w", encoding="utf-8") as f:
        for word, freq in sorted(words.items(), key=lambda x: x[1], reverse=True):
            patterns = generate_mojibake_patterns(word)
            for pattern in patterns:
                f.write(f"{word}|{pattern}|{freq}\n")
    
    print(f"Extracted {len(words)} words with accents")