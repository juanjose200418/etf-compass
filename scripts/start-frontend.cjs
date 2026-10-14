const { spawn } = require('node:child_process');
const http = require('node:http');
const net = require('node:net');
const path = require('node:path');

const HOST = 'localhost';
const PORT = 4200;
const URL = `http://${HOST}:${PORT}`;

function canConnect() {
  return new Promise(resolve => {
    const socket = net.createConnection({ host: HOST, port: PORT });
    socket.setTimeout(1000);
    socket.once('connect', () => {
      socket.destroy();
      resolve(true);
    });
    socket.once('timeout', () => {
      socket.destroy();
      resolve(false);
    });
    socket.once('error', () => resolve(false));
  });
}

function readExistingServer() {
  return new Promise(resolve => {
    const request = http.get(URL, response => {
      let body = '';
      response.setEncoding('utf8');
      response.on('data', chunk => {
        body += chunk;
        if (body.length > 4096) {
          request.destroy();
        }
      });
      response.on('end', () => resolve({ statusCode: response.statusCode, body }));
    });

    request.setTimeout(1500, () => request.destroy());
    request.on('error', () => resolve(null));
  });
}

async function main() {
  if (await canConnect()) {
    const existing = await readExistingServer();
    const isEtfCompass = existing?.statusCode === 200 && /ETF Compass|app-root|@vite\/client/.test(existing.body);

    if (isEtfCompass) {
      console.log(`ETF Compass frontend is already running at ${URL}. Reusing it; not starting another dev server.`);
      return;
    }

    console.error(`Port ${PORT} is already in use, but it does not look like ETF Compass. Stop that process before running npm start.`);
    process.exit(1);
  }

  const ngBin = path.join(__dirname, '..', 'node_modules', '@angular', 'cli', 'bin', 'ng.js');
  const child = spawn(process.execPath, [ngBin, 'serve', '--host', HOST, '--port', String(PORT), '--proxy-config', 'proxy.conf.json'], {
    cwd: path.join(__dirname, '..'),
    stdio: 'inherit'
  });

  child.on('exit', code => process.exit(code ?? 0));
  child.on('error', error => {
    console.error(error.message);
    process.exit(1);
  });
}

main().catch(error => {
  console.error(error.message);
  process.exit(1);
});
