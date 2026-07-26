import zipfile
import xml.etree.ElementTree as ET

def extract_docx_text(docx_path, output_txt_path):
    # Namespace dictionary to find elements
    ns = {'w': 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'}
    
    with zipfile.ZipFile(docx_path) as docx:
        tree = ET.parse(docx.open('word/document.xml'))
        root = tree.getroot()
        
        paragraphs = []
        for p in root.iter('{http://schemas.openxmlformats.org/wordprocessingml/2006/main}p'):
            texts = []
            for t in p.iter('{http://schemas.openxmlformats.org/wordprocessingml/2006/main}t'):
                if t.text:
                    texts.append(t.text)
            paragraphs.append(''.join(texts))
            
        with open(output_txt_path, 'w', encoding='utf-8') as f:
            f.write('\n'.join(paragraphs))

extract_docx_text('NOUS-Module-Strategy-2-14.docx', 'nous-modules-strategy.txt')
print("Successfully extracted docx text!")
