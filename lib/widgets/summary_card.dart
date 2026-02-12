import 'package:flutter/material.dart';

class SummaryCard extends StatelessWidget {
  final IconData icon;
  final String title;
  final String value;
  final Color color;
  final Color iconColor;
  final bool fullWidth;

  const SummaryCard({
    super.key,
    required this.icon,
    required this.title,
    required this.value,
    required this.color,
    required this.iconColor,
    this.fullWidth = false,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      width: fullWidth ? double.infinity : null,
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 16), // Menos padding horizontal
      decoration: BoxDecoration(
        color: color,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Colors.black12),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.spaceBetween, // Distribuye espacio
        children: [
          Row(
            children: [
              CircleAvatar(
                radius: 16, // Icono un poco más pequeño
                backgroundColor: iconColor.withOpacity(0.1),
                child: Icon(icon, color: iconColor, size: 18),
              ),
              const SizedBox(width: 8),
              // Flexible evita que el título rompa la tarjeta
              Flexible(
                child: Text(
                  title,
                  style: const TextStyle(color: Colors.grey, fontSize: 13, fontWeight: FontWeight.bold),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          // FittedBox hace que si el número es gigante (S/10,000.00), se achique la letra para caber
          FittedBox(
            fit: BoxFit.scaleDown,
            alignment: Alignment.centerLeft,
            child: Text(
              value,
              style: const TextStyle(fontSize: 26, fontWeight: FontWeight.bold),
            ),
          ),
        ],
      ),
    );
  }
}