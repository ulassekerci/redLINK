import { defineTask, TaskManagerError } from 'expo-task-manager'
import { LocationUpdate } from './interfaces'
import { useVehicleStore } from '../../store/vehicle'

export const createLocationTask = () => {
  defineTask('updateGPS', async ({ data, error }: { data: LocationUpdate; error: TaskManagerError | null }) => {
    if (error) return
    if (data) useVehicleStore.setState({ location: data.locations[0] })
  })
}
