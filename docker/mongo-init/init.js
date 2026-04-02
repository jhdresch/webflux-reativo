// pega variáveis de ambiente corretamente (em mongosh use process.env)
const database = (typeof process !== 'undefined' && process.env && process.env.MONGO_INITDB_DATABASE) ? process.env.MONGO_INITDB_DATABASE : 'admin';
const appUser = (typeof process !== 'undefined' && process.env && process.env.APP_MONGO_USER) ? process.env.APP_MONGO_USER : null;
const appPassword = (typeof process !== 'undefined' && process.env && process.env.APP_MONGO_PASSWORD) ? process.env.APP_MONGO_PASSWORD : null;

print('Database:', database);
print('User:', appUser);

// conecta no database alvo (onde a aplicação irá operar)
const targetDb = db.getSiblingDB(database);

// cria usuário da aplicação NO DB 'admin' (authSource 'admin' é o padrão usado pela app)
if (appUser && appPassword) {
  const adminDb = db.getSiblingDB('admin');
  adminDb.createUser({
    user: appUser,
    pwd: appPassword,
    roles: [
      {
        role: 'readWrite',
        db: database
      }
    ]
  });
  print('✅ Application user created in admin DB with readWrite on', database);
} else {
  print('⚠️  APP_MONGO_USER or APP_MONGO_PASSWORD not provided — skipping application user creation');
}
