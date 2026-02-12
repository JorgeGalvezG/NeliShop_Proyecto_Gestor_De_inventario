import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';

class AnimatedListItem extends StatelessWidget {
  final int index;
  final Widget child;

  const AnimatedListItem({
    super.key,
    required this.index,
    required this.child,
  });

  @override
  Widget build(BuildContext context) {
    // Animación en cascada: Cada elemento aparece con un ligero retraso según su índice
    return child
        .animate()
        .fadeIn(duration: 400.ms, delay: (index * 50).ms)
        .slideX(begin: 0.1, end: 0, curve: Curves.easeOutQuad);
  }
}