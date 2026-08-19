mkdir -p secrets
openssl rand -base64 32 > secrets/postgres_password.txt
openssl rand -base64 48 > secrets/app_secret_key.txt
openssl rand -base64 32 > secrets/adminpassword.txt


if [[ -z $(grep 'secrets' .gitignore) ]]; then
  printf "secrets/\n.env\n*.env\n" >> .gitignore
fi
