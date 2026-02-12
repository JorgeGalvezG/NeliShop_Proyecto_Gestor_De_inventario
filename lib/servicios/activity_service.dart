import 'package:flutter/foundation.dart';
import '../modelos/activity_event_model.dart';

// Este servicio es el "cerebro" que almacena la lista de actividades
// para que 'home_page.dart' la pueda leer.
class ActivityService {
  // Singleton
  ActivityService._privateConstructor();
  static final ActivityService instance = ActivityService._privateConstructor();

  // Notificador para que la UI reaccione a los cambios
  final ValueNotifier<List<ActivityEvent>> activityNotifier =
  ValueNotifier<List<ActivityEvent>>([]);

  // Método para añadir una nueva actividad (llamado desde Ventas y Compras)
  void addActivity(ActivityEvent event) {
    final currentActivities = activityNotifier.value;
    // Añade la nueva actividad AL INICIO de la lista
    activityNotifier.value = [event, ...currentActivities];
  }

  // Método para obtener la lista
  List<ActivityEvent> getActivities() {
    return activityNotifier.value;
  }
}