import 'package:flutter/material.dart';

class StitchLoader extends StatelessWidget {
  final double size;
  final double strokeWidth;
  final Color? color;

  const StitchLoader({
    super.key,
    this.size = 50.0,        // Tamaño del círculo
    this.strokeWidth = 5.0,  // Grosor de la línea
    this.color,              // Color opcional (si no, usa el del tema)
  });

  @override
  Widget build(BuildContext context) {
    return Center(
      child: SizedBox(
        width: size,
        height: size,
        child: CircularProgressIndicator(
          strokeWidth: strokeWidth,
          strokeCap: StrokeCap.round, // Esto hace que las puntas sean redonditas (más bonito)
          valueColor: AlwaysStoppedAnimation<Color>(
            color ?? Theme.of(context).colorScheme.primary, // Usa tu color Teal/Verde
          ),
          backgroundColor: Colors.grey[200], // Un fondo gris suave para que se note el camino
        ),
      ),
    );
  }
}