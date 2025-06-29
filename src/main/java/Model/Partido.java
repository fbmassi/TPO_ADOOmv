package Model;

import Strategy.EstrategiaEmparejamiento;
import DTO.PartidoDTO;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import State.EstadoPartido;
import State.EstadoBuscandoJugadores;
import State.EstadoPartidoArmado;
import State.EstadoPartidoConfirmado;
import State.EstadoPartidoEnJuego;
import State.EstadoPartidoFinalizado;
import State.EstadoPartidoCancelado;

@Getter
@Setter
public class Partido {
	private int id;
	private Deporte deporte;
	private int cantidadJugadoresRequeridos;
	private int duracion;
	private Nivel nivelMaximo;
	private Nivel nivelMinimo;
	private Ubicacion ubicacion;
	private EstadoPartido estado;
	private List<Usuario> jugadoresAnotados;
	private Set<Usuario> jugadoresConfirmados;
	private EstrategiaEmparejamiento estrategiaEmparejamiento;
	private List<Notificador> observadores;
	private Usuario organizador;
	private LocalDateTime fechaHora;
	private int minPartidosRequeridos;

	public Partido(Ubicacion ubicacion, Deporte deporte, int cantidadJugadoresRequeridos,
                  int duracion, Usuario organizador, EstrategiaEmparejamiento estrategia,
                  LocalDateTime fechaHora) {
		this.ubicacion = ubicacion;
		this.deporte = deporte;
		this.cantidadJugadoresRequeridos = cantidadJugadoresRequeridos;
		this.duracion = duracion;
		this.organizador = organizador;
		this.estrategiaEmparejamiento = estrategia;
		this.jugadoresAnotados = new ArrayList<>();
		this.jugadoresConfirmados = new HashSet<>();
		this.observadores = new ArrayList<>();
		this.fechaHora = fechaHora;
		this.jugadoresAnotados.add(organizador);
		// El organizador se considera confirmado automáticamente
		this.jugadoresConfirmados.add(organizador);
		this.nivelMinimo = Nivel.PRINCIPIANTE;
		this.nivelMaximo = Nivel.AVANZADO;
		this.minPartidosRequeridos = 0;

		// Inicialización del estado con contexto
		this.estado = new EstadoBuscandoJugadores();
		this.estado.setContexto(this);
	}

	public void agregarJugador(Usuario jugador) {
		if (!jugadoresAnotados.contains(jugador)) {
			estado.agregarJugador(jugador);
		} else {
			System.out.println("El jugador ya está anotado en este partido.");
		}
	}

	public void eliminarJugador(Usuario jugador) {
		if (jugadoresAnotados.contains(jugador)) {
			estado.eliminarJugador(jugador);
			jugadoresConfirmados.remove(jugador); // Si se elimina un jugador, también eliminar su confirmación
		} else {
			System.out.println("El jugador no está anotado en este partido.");
		}
	}

	public void cambiarEstado(EstadoPartido nuevoEstado) {
		this.estado = nuevoEstado;
		this.estado.setContexto(this);  // Establecer el contexto en el nuevo estado
		this.notificar();
	}

	public void cancelarPartido() {
		estado.cancelarPartido();
	}

	public void confirmarJugador(Usuario jugador) {
		if (jugadoresAnotados.contains(jugador)) {
			jugadoresConfirmados.add(jugador);
			System.out.println("Jugador " + jugador.getNombreUsuario() + " confirmado para el partido.");
			verificarConfirmaciones();
		} else {
			System.out.println("No se puede confirmar un jugador que no está anotado.");
		}
	}

	private void verificarConfirmaciones() {
		// Solo verificar confirmaciones si el partido está en estado armado
		if (estado instanceof EstadoPartidoArmado) {
			if (jugadoresConfirmados.size() == jugadoresAnotados.size() &&
				jugadoresAnotados.size() >= cantidadJugadoresRequeridos) {
				estado.confirmarJugador();
			}
		}
	}

	public void notificar() {
		String mensaje = "Actualización del partido de " + deporte.getNombre() +
                    " en " + ubicacion.getDireccion() +
                    ". Estado: " + estado.getNombreEstado();

		for (Notificador n : observadores) {
			for (Usuario jugador : jugadoresAnotados) {
				n.notificar(jugador, mensaje);
			}
		}
	}

	public void editarPartido(Ubicacion ubicacion, int duracion, LocalDateTime fechaHora) {
		this.ubicacion = ubicacion;
		this.duracion = duracion;
		this.fechaHora = fechaHora;
	}

	public void verificarEstadoActual() {
		LocalDateTime ahora = LocalDateTime.now();

		// Si llegó la hora del partido y está confirmado, pasará al estado "En juego"
		if (ahora.isAfter(fechaHora) && estado instanceof EstadoPartidoConfirmado) {
			cambiarEstado(new EstadoPartidoEnJuego());
			System.out.println("El partido ha comenzado y pasa a estado En Juego!");
		}

		// Si terminó el partido, pasará al estado "Finalizado"
		if (estado instanceof EstadoPartidoEnJuego &&
			ahora.isAfter(fechaHora.plusMinutes(duracion))) {
			cambiarEstado(new EstadoPartidoFinalizado());
			System.out.println("El partido ha finalizado!");
		}
	}

	public void confirmarJugadores() {
		estado.confirmarJugador();
	}

	public void agregarObservador(Notificador observador) {
		if (!observadores.contains(observador)) {
			observadores.add(observador);
		}
	}

	public void eliminarObservador(Notificador observador) {
		observadores.remove(observador);
	}

	public PartidoDTO toDTO() {
		PartidoDTO dto = new PartidoDTO();
		dto.setId(String.valueOf(this.id));
		dto.setNombreDeporte(this.deporte.getNombre());
		dto.setEstado(this.estado.getNombreEstado());
		dto.setUbicacion(this.ubicacion != null ? this.ubicacion.toDTO() : null);
		dto.setFechaHora(this.fechaHora.toString());
		dto.setCantidadJugadoresAnotados(this.jugadoresAnotados.size());
		dto.setCantidadJugadoresRequeridos(this.cantidadJugadoresRequeridos);
		dto.setDuracion(this.duracion);
		return dto;
	}
}
