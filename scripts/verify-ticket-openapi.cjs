const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');

const [sourceArgument, generatedArgument] = process.argv.slice(2);

if (!sourceArgument || !generatedArgument) {
  throw new Error('Usage: node verify-ticket-openapi.cjs SOURCE GENERATED');
}

const sourceFile = path.resolve(sourceArgument);
const generatedFile = path.resolve(generatedArgument);
const source = JSON.parse(fs.readFileSync(sourceFile, 'utf8'));
const generated = JSON.parse(fs.readFileSync(generatedFile, 'utf8'));
const methods = new Set(['get', 'post', 'put', 'patch', 'delete', 'options', 'head', 'trace']);

function resolvePointer(document, reference) {
  return reference.slice(2).split('/').reduce(
    (current, token) => current[token.replaceAll('~1', '/').replaceAll('~0', '~')],
    document,
  );
}

function normalizeText(value) {
  return value.replace(/\s+/gu, ' ').trim();
}

function normalize(document, value, references = new Set()) {
  if (Array.isArray(value)) {
    return value.map(item => normalize(document, item, references));
  }
  if (!value || typeof value !== 'object') {
    return typeof value === 'string' ? normalizeText(value) : value;
  }
  if (Object.keys(value).length === 1 && value.$ref?.startsWith('#/')) {
    if (references.has(value.$ref)) {
      return {$ref: value.$ref};
    }
    return normalize(
      document,
      resolvePointer(document, value.$ref),
      new Set([...references, value.$ref]),
    );
  }

  const result = {};
  for (const key of Object.keys(value).sort()) {
    // Examples are documentation samples, not executable API constraints.
    if (key === 'example' || key === 'examples' || key.startsWith('x-')) {
      continue;
    }
    result[key] = normalize(document, value[key], references);
  }

  // JSON object member order and the order of required names have no semantics.
  if (Array.isArray(result.required)) {
    result.required.sort();
  }
  if (Array.isArray(result.parameters)) {
    result.parameters.sort((left, right) =>
      `${left.in}:${left.name}`.localeCompare(`${right.in}:${right.name}`));
  }
  if (result.type === 'array'
      && result.description
      && result.items?.description === result.description) {
    delete result.items.description;
  }
  return result;
}

function mergedParameters(pathItem, operation) {
  return [...(pathItem.parameters || []), ...(operation.parameters || [])]
    .sort((left, right) => {
      const leftValue = left.$ref || `${left.in}:${left.name}`;
      const rightValue = right.$ref || `${right.in}:${right.name}`;
      return leftValue.localeCompare(rightValue);
    });
}

function executableDocument(document) {
  const paths = {};
  for (const pathName of Object.keys(document.paths || {}).sort()) {
    const pathItem = document.paths[pathName];
    paths[pathName] = {};
    for (const method of Object.keys(pathItem).filter(key => methods.has(key)).sort()) {
      const operation = structuredClone(pathItem[method]);
      const parameters = mergedParameters(pathItem, operation);
      if (parameters.length > 0) {
        operation.parameters = parameters;
      } else {
        delete operation.parameters;
      }
      paths[pathName][method] = operation;
    }
  }

  return normalize(document, {
    openapi: document.openapi,
    info: document.info,
    servers: document.servers,
    tags: document.tags,
    security: document.security,
    paths,
  });
}

const sourceContract = executableDocument(source);
const generatedContract = executableDocument(generated);
const outputDirectory = path.join(path.dirname(generatedFile), 'comparison');
fs.mkdirSync(outputDirectory, {recursive: true});
fs.writeFileSync(
  path.join(outputDirectory, 'source.contract.json'),
  `${JSON.stringify(sourceContract, null, 2)}\n`,
);
fs.writeFileSync(
  path.join(outputDirectory, 'generated.contract.json'),
  `${JSON.stringify(generatedContract, null, 2)}\n`,
);

try {
  assert.deepStrictEqual(generatedContract, sourceContract);
} catch (error) {
  throw new Error(
    `Ticket API contract differs. Compare normalized files in ${outputDirectory}.`,
    {cause: error},
  );
}

console.log(`Ticket API contract matches ${sourceFile}.`);
console.log('Verified: operations, parameters, schemas, response codes and response headers.');
console.log(`Generated specification: ${generatedFile}`);
