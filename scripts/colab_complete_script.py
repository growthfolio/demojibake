#!/usr/bin/env python3

import struct
import hashlib
from collections import defaultdict

def generate_all_mojibake_patterns(word):
    """Gera TODOS os padrões possíveis de mojibake para uma palavra"""
    
    patterns = set()
    
    # Padrão 1: UTF-8 -> ISO-8859-1 (mais comum)
    mojibake_basic = {
        'á': '?', 'à': '?', 'â': '?', 'ã': '?',
        'é': '?', 'ê': '?', 'í': '?', 'ó': '?',
        'ô': '?', 'õ': '?', 'ú': '?', 'ç': '??'
    }
    
    # Padrão 2: UTF-8 -> Windows-1252
    mojibake_windows = {
        'á': 'Ã¡', 'à': 'Ã ', 'â': 'Ã¢', 'ã': 'Ã£',
        'é': 'Ã©', 'ê': 'Ãª', 'í': 'Ã­', 'ó': 'Ã³',
        'ô': 'Ã´', 'õ': 'Ãµ', 'ú': 'Ãº', 'ç': 'Ã§'
    }
    
    # Padrão 3: Sem acentos
    no_accents = {
        'á': 'a', 'à': 'a', 'â': 'a', 'ã': 'a',
        'é': 'e', 'ê': 'e', 'í': 'i', 'ó': 'o',
        'ô': 'o', 'õ': 'o', 'ú': 'u', 'ç': 'c'
    }
    
    # Gera variações
    for mapping in [mojibake_basic, mojibake_windows, no_accents]:
        broken = word
        for correct, replacement in mapping.items():
            broken = broken.replace(correct, replacement)
        
        if broken != word:
            patterns.add(broken)
    
    return list(patterns)

def build_comprehensive_dictionary(input_file="palavras_com_especiais.txt", output_file="dictionary_complete.txt"):
    """Constrói dicionário com TODOS os padrões"""
    
    total_entries = 0
    processed_words = 0
    
    print("Processando arquivo...")
    
    with open(input_file, 'r', encoding='utf-8') as f_in:
        with open(output_file, 'w', encoding='utf-8') as f_out:
            
            for line_num, line in enumerate(f_in):
                word = line.strip().lower()
                
                if not word or len(word) < 3:
                    continue
                
                # Gera todos os padrões possíveis
                patterns = generate_all_mojibake_patterns(word)
                
                for pattern in patterns:
                    f_out.write(f"{word}|{pattern}\n")
                    total_entries += 1
                
                processed_words += 1
                
                # Progress feedback
                if processed_words % 10000 == 0:
                    print(f"Processadas {processed_words} palavras, {total_entries} entradas geradas")
    
    print(f"Dicionário completo: {total_entries} entradas de {processed_words} palavras")
    return total_entries

def build_binary_dictionary(text_file="dictionary_complete.txt", binary_file="dictionary_470k.bin"):
    """Constrói dicionário binário otimizado"""
    
    entries = []
    string_buffer = b""
    
    print("Construindo dicionário binário...")
    
    with open(text_file, 'r', encoding='utf-8') as f:
        for line_num, line in enumerate(f):
            if '|' not in line:
                continue
                
            correct, broken = line.strip().split('|', 1)
            
            # Adiciona strings ao buffer
            correct_offset = len(string_buffer)
            correct_bytes = correct.encode('utf-8')
            string_buffer += correct_bytes + b'\0'
            
            broken_offset = len(string_buffer)
            broken_bytes = broken.encode('utf-8')
            string_buffer += broken_bytes + b'\0'
            
            # Hash para lookup rápido
            broken_hash = hash(broken) & 0xFFFFFFFF
            
            entries.append({
                'broken_hash': broken_hash,
                'correct_offset': correct_offset,
                'broken_offset': broken_offset,
                'correct_len': len(correct_bytes),
                'broken_len': len(broken_bytes)
            })
            
            if line_num % 50000 == 0:
                print(f"Processadas {line_num} linhas...")
    
    # Ordena por hash para binary search
    print("Ordenando entradas...")
    entries.sort(key=lambda x: x['broken_hash'])
    
    # Escreve arquivo binário
    print("Escrevendo arquivo binário...")
    with open(binary_file, 'wb') as f:
        # Header (16 bytes)
        f.write(b'DICT')  # Magic (4 bytes)
        f.write(struct.pack('<I', 1))  # Version (4 bytes)
        f.write(struct.pack('<I', len(entries)))  # Num entries (4 bytes)
        f.write(struct.pack('<I', len(string_buffer)))  # String buffer size (4 bytes)
        
        # Entries (20 bytes each)
        for entry in entries:
            f.write(struct.pack('<IIIHH', 
                entry['broken_hash'],
                entry['correct_offset'],
                entry['broken_offset'],
                entry['correct_len'],
                entry['broken_len']
            ))
        
        # String buffer
        f.write(string_buffer)
    
    print(f"Dicionário binário criado: {len(entries)} entradas")
    print(f"Tamanho: {len(string_buffer) // 1024}KB strings + {len(entries) * 20 // 1024}KB índice")
    print(f"Total: {(len(string_buffer) + len(entries) * 20) // 1024}KB")

def test_patterns():
    """Testa geração de padrões com palavras exemplo"""
    
    test_words = ["ação", "não", "produção", "há", "coração", "informação"]
    
    print("Testando padrões de mojibake:")
    for word in test_words:
        patterns = generate_all_mojibake_patterns(word)
        print(f"\n'{word}' -> {patterns}")

if __name__ == "__main__":
    print("=== Processador de Dicionário PT-BR ===")
    
    # 1. Testa padrões
    test_patterns()
    
    # 2. Processa arquivo completo
    print("\n=== Processando arquivo completo ===")
    total_entries = build_comprehensive_dictionary()
    
    # 3. Gera binário otimizado
    print("\n=== Gerando binário otimizado ===")
    build_binary_dictionary()
    
    print("\n✅ Processamento concluído!")
    print(f"📊 Total de entradas: {total_entries}")
    print("📁 Arquivos gerados:")
    print("  - dictionary_complete.txt (texto)")
    print("  - dictionary_470k.bin (binário otimizado)")