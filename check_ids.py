import os, re

java_dir = r'c:\Apps Dev\AndroidStudioProjects\Eudaimonia_Forum\app\src\main\java'
res_dir = r'c:\Apps Dev\AndroidStudioProjects\Eudaimonia_Forum\app\src\main\res'

java_ids = set()
xml_ids = set()

for root, _, files in os.walk(java_dir):
    for file in files:
        if file.endswith('.java'):
            path = os.path.join(root, file)
            with open(path, 'r', encoding='utf-8') as f:
                content = f.read()
                matches = re.findall(r'R\.id\.([a-zA-Z0-9_]+)', content)
                for m in matches:
                    java_ids.add((m, file))

for root, _, files in os.walk(res_dir):
    for file in files:
        if file.endswith('.xml'):
            path = os.path.join(root, file)
            with open(path, 'r', encoding='utf-8') as f:
                content = f.read()
                matches = re.findall(r'"@\+?id/([a-zA-Z0-9_]+)"', content)
                for m in matches:
                    xml_ids.add(m)

print(f"Total Java R.id uses found: {len(java_ids)}")
print(f"Total XML IDs found: {len(xml_ids)}")

missing = []
for jid, file in java_ids:
    if jid not in xml_ids:
        missing.append((jid, file))

if not missing:
    print('All IDs found in Java have a corresponding XML definition.')
else:
    print('The following IDs are used in Java but missing from XML:')
    for jid, file in sorted(missing, key=lambda x: x[1]):
        print(f'{file}: R.id.{jid}')
