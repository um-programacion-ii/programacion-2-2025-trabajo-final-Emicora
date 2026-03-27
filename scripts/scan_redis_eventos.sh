#!/bin/bash

# Script para escanear eventos desde Redis
# Uso: ./scan_redis_eventos.sh [evento_id]
# Si no se especifica evento_id, escanea todos los eventos

REDIS_HOST="${REDIS_HOST:-192.168.194.250}"
REDIS_PORT="${REDIS_PORT:-6379}"
EVENTO_ID="$1"

echo "=========================================="
echo "Escaneando eventos en Redis"
echo "Host: $REDIS_HOST"
echo "Puerto: $REDIS_PORT"
echo "=========================================="
echo ""

# Verificar si redis-cli está instalado
if ! command -v redis-cli &> /dev/null; then
    echo "ERROR: redis-cli no está instalado."
    echo "Instálalo con: sudo apt-get install redis-tools"
    exit 1
fi

# Si se especifica un evento_id, mostrar solo ese evento
if [ -n "$EVENTO_ID" ]; then
    KEY="evento_$EVENTO_ID"
    echo "=== Consultando evento: $KEY ==="
    echo ""
    
    # Verificar si la clave existe
    EXISTS=$(redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" EXISTS "$KEY")
    if [ "$EXISTS" -eq 0 ]; then
        echo "⚠️  La clave '$KEY' no existe en Redis"
        exit 1
    fi
    
    # Obtener el valor
    VALUE=$(redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" GET "$KEY")
    
    # Intentar formatear con jq si está disponible
    if command -v jq &> /dev/null; then
        echo "$VALUE" | jq .
    else
        echo "$VALUE"
    fi
    
    echo ""
    echo "=== Resumen ==="
    echo "Clave: $KEY"
    TTL=$(redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" TTL "$KEY")
    if [ "$TTL" -eq -1 ]; then
        echo "TTL: Sin expiración"
    elif [ "$TTL" -eq -2 ]; then
        echo "TTL: Clave no existe"
    else
        echo "TTL: $TTL segundos"
    fi
else
    # Escanear todos los eventos
    echo "Escaneando todas las claves que empiezan con 'evento_'..."
    echo ""
    
    # Usar SCAN para obtener todas las claves
    CURSOR=0
    COUNT=0
    
    while true; do
        RESULT=$(redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" SCAN "$CURSOR" MATCH "evento_*" COUNT 100)
        CURSOR=$(echo "$RESULT" | head -n 1)
        KEYS=$(echo "$RESULT" | tail -n +2)
        
        if [ -z "$KEYS" ]; then
            break
        fi
        
        while IFS= read -r KEY; do
            if [ -n "$KEY" ]; then
                COUNT=$((COUNT + 1))
                echo "[$COUNT] $KEY"
                
                # Obtener un resumen rápido
                VALUE=$(redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" GET "$KEY")
                
                # Contar asientos bloqueados y ocupados (si jq está disponible)
                if command -v jq &> /dev/null && [ -n "$VALUE" ]; then
                    BLOQUEADOS=$(echo "$VALUE" | jq '[.asientos[]? | select(.estado == "Bloqueado" or .estado == "BLOQUEADO")] | length' 2>/dev/null || echo "?")
                    OCUPADOS=$(echo "$VALUE" | jq '[.asientos[]? | select(.estado == "Vendido" or .estado == "VENDIDO" or .estado == "Ocupado" or .estado == "OCUPADO")] | length' 2>/dev/null || echo "?")
                    TOTAL=$(echo "$VALUE" | jq '.asientos | length' 2>/dev/null || echo "?")
                    echo "    → Total asientos: $TOTAL | Bloqueados: $BLOQUEADOS | Ocupados: $OCUPADOS"
                fi
            fi
        done <<< "$KEYS"
        
        if [ "$CURSOR" -eq 0 ]; then
            break
        fi
    done
    
    echo ""
    echo "=== Resumen ==="
    echo "Total de eventos encontrados: $COUNT"
    echo ""
    echo "Para ver el detalle de un evento específico:"
    echo "  ./scan_redis_eventos.sh <evento_id>"
    echo ""
    echo "Ejemplo: ./scan_redis_eventos.sh 1"
fi

