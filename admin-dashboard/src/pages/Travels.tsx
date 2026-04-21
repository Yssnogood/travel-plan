import { useState } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Typography,
  IconButton,
  Chip,
  MenuItem,
  Alert,
  Snackbar,
} from '@mui/material';
import { DataGrid, GridColDef, GridRenderCellParams } from '@mui/x-data-grid';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Visibility as ViewIcon,
} from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm, Controller } from 'react-hook-form';
import { AxiosError } from 'axios';
import { travelService, Travel, CreateTravelRequest, UpdateTravelRequest } from '../api/travelService';

const STATUS_OPTIONS = [
  { value: 'DRAFT', label: 'Brouillon', color: 'default' },
  { value: 'PLANNED', label: 'Planifié', color: 'info' },
  { value: 'IN_PROGRESS', label: 'En cours', color: 'primary' },
  { value: 'COMPLETED', label: 'Terminé', color: 'success' },
  { value: 'CANCELLED', label: 'Annulé', color: 'error' },
];

export default function Travels() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [selectedTravel, setSelectedTravel] = useState<Travel | null>(null);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' as 'success' | 'error' });

  const { register, handleSubmit, control, reset, formState: { errors } } = useForm<CreateTravelRequest>();

  const getApiErrorMessage = (error: unknown, fallback: string) => {
    const axiosError = error as AxiosError<{ message?: string }>;
    return axiosError.response?.data?.message || fallback;
  };

  const { data: travelsData, isLoading } = useQuery({
    queryKey: ['travels', page, pageSize],
    queryFn: () => travelService.getAll({ page, size: pageSize }),
  });

  const createMutation = useMutation({
    mutationFn: (data: CreateTravelRequest) => travelService.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['travels'] });
      setDialogOpen(false);
      reset();
      setSnackbar({ open: true, message: 'Voyage créé avec succès', severity: 'success' });
    },
    onError: (error) => {
      setSnackbar({ open: true, message: getApiErrorMessage(error, 'Erreur lors de la création'), severity: 'error' });
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateTravelRequest }) => travelService.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['travels'] });
      setDialogOpen(false);
      setSelectedTravel(null);
      reset();
      setSnackbar({ open: true, message: 'Voyage mis à jour', severity: 'success' });
    },
    onError: (error) => {
      setSnackbar({ open: true, message: getApiErrorMessage(error, 'Erreur lors de la mise à jour'), severity: 'error' });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => travelService.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['travels'] });
      setDeleteDialogOpen(false);
      setSelectedTravel(null);
      setSnackbar({ open: true, message: 'Voyage supprimé', severity: 'success' });
    },
    onError: (error) => {
      setSnackbar({ open: true, message: getApiErrorMessage(error, 'Erreur lors de la suppression'), severity: 'error' });
    },
  });

  const getStatusConfig = (status: string) => {
    return STATUS_OPTIONS.find((s) => s.value === status) || STATUS_OPTIONS[0];
  };

  const columns: GridColDef[] = [
    { field: 'id', headerName: 'ID', width: 70 },
    { field: 'title', headerName: 'Titre', width: 200 },
    { field: 'userId', headerName: 'User ID', width: 90 },
    {
      field: 'startDate',
      headerName: 'Début',
      width: 120,
      renderCell: (params: GridRenderCellParams<Travel>) => {
        const value = params.row?.startDate;
        if (!value) return '-';
        return new Date(value).toLocaleDateString('fr-FR');
      },
    },
    {
      field: 'endDate',
      headerName: 'Fin',
      width: 120,
      renderCell: (params: GridRenderCellParams<Travel>) => {
        const value = params.row?.endDate;
        if (!value) return '-';
        return new Date(value).toLocaleDateString('fr-FR');
      },
    },
    {
      field: 'budget',
      headerName: 'Budget',
      width: 120,
      renderCell: (params: GridRenderCellParams<Travel>) => {
        const value = params.row?.budget;
        if (value == null) return '-';
        const currency = params.row?.currency || 'EUR';
        return `${value.toLocaleString()} ${currency}`;
      },
    },
    {
      field: 'status',
      headerName: 'Statut',
      width: 130,
      renderCell: (params: GridRenderCellParams) => {
        const config = getStatusConfig(params.value);
        return (
          <Chip
            label={config.label}
            color={config.color as any}
            size="small"
          />
        );
      },
    },
    {
      field: 'destinations',
      headerName: 'Destinations',
      width: 130,
      valueGetter: (value: any[]) => value?.length || 0,
    },
    {
      field: 'actions',
      headerName: 'Actions',
      width: 130,
      sortable: false,
      renderCell: (params: GridRenderCellParams<Travel>) => (
        <Box>
          <IconButton size="small">
            <ViewIcon fontSize="small" />
          </IconButton>
          <IconButton
            size="small"
            onClick={() => {
              setSelectedTravel(params.row);
              setDialogOpen(true);
            }}
          >
            <EditIcon fontSize="small" />
          </IconButton>
          <IconButton
            size="small"
            color="error"
            onClick={() => {
              setSelectedTravel(params.row);
              setDeleteDialogOpen(true);
            }}
          >
            <DeleteIcon fontSize="small" />
          </IconButton>
        </Box>
      ),
    },
  ];

  const onSubmit = (data: CreateTravelRequest) => {
    if (selectedTravel) {
      updateMutation.mutate({ id: selectedTravel.id, data });
    } else {
      createMutation.mutate(data);
    }
  };

  const handleOpenDialog = () => {
    setSelectedTravel(null);
    reset({});
    setDialogOpen(true);
  };

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4" fontWeight="bold">
          Voyages
        </Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={handleOpenDialog}>
          Nouveau voyage
        </Button>
      </Box>

      <Card>
        <CardContent>
          <DataGrid
            rows={travelsData?.data || []}
            columns={columns}
            loading={isLoading}
            paginationMode="server"
            rowCount={travelsData?.pageInfo?.totalElements || 0}
            pageSizeOptions={[5, 10, 25]}
            paginationModel={{ page, pageSize }}
            onPaginationModelChange={(model) => {
              setPage(model.page);
              setPageSize(model.pageSize);
            }}
            autoHeight
            disableRowSelectionOnClick
          />
        </CardContent>
      </Card>

      {/* Create/Edit Dialog */}
      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth>
        <form onSubmit={handleSubmit(onSubmit)}>
          <DialogTitle>
            {selectedTravel ? 'Modifier le voyage' : 'Nouveau voyage'}
          </DialogTitle>
          <DialogContent>
            <TextField
              fullWidth
              label="Titre"
              margin="normal"
              {...register('title', { required: 'Titre requis' })}
              error={!!errors.title}
              helperText={errors.title?.message}
              defaultValue={selectedTravel?.title}
            />
            <TextField
              fullWidth
              label="Description"
              margin="normal"
              multiline
              rows={3}
              {...register('description')}
              defaultValue={selectedTravel?.description}
            />
            <TextField
              fullWidth
              label="Date de début"
              type="date"
              margin="normal"
              InputLabelProps={{ shrink: true }}
              {...register('startDate', { required: 'Date de début requise' })}
              error={!!errors.startDate}
              helperText={errors.startDate?.message}
              defaultValue={selectedTravel?.startDate?.split('T')[0]}
            />
            <TextField
              fullWidth
              label="Date de fin"
              type="date"
              margin="normal"
              InputLabelProps={{ shrink: true }}
              {...register('endDate', { required: 'Date de fin requise' })}
              error={!!errors.endDate}
              helperText={errors.endDate?.message}
              defaultValue={selectedTravel?.endDate?.split('T')[0]}
            />
            <TextField
              fullWidth
              label="Budget"
              type="number"
              margin="normal"
              {...register('budget')}
              defaultValue={selectedTravel?.budget}
            />
            <TextField
              fullWidth
              label="Devise"
              margin="normal"
              {...register('currency')}
              defaultValue={selectedTravel?.currency || 'EUR'}
            />
            {selectedTravel && (
              <Controller
                name="status"
                control={control}
                defaultValue={selectedTravel?.status}
                render={({ field }) => (
                  <TextField
                    {...field}
                    select
                    fullWidth
                    label="Statut"
                    margin="normal"
                  >
                    {STATUS_OPTIONS.map((option) => (
                      <MenuItem key={option.value} value={option.value}>
                        {option.label}
                      </MenuItem>
                    ))}
                  </TextField>
                )}
              />
            )}
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setDialogOpen(false)}>Annuler</Button>
            <Button type="submit" variant="contained">
              {selectedTravel ? 'Modifier' : 'Créer'}
            </Button>
          </DialogActions>
        </form>
      </Dialog>

      {/* Delete Confirmation Dialog */}
      <Dialog open={deleteDialogOpen} onClose={() => setDeleteDialogOpen(false)}>
        <DialogTitle>Confirmer la suppression</DialogTitle>
        <DialogContent>
          <Typography>
            Êtes-vous sûr de vouloir supprimer le voyage "{selectedTravel?.title}" ?
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteDialogOpen(false)}>Annuler</Button>
          <Button
            color="error"
            variant="contained"
            onClick={() => selectedTravel && deleteMutation.mutate(selectedTravel.id)}
          >
            Supprimer
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar
        open={snackbar.open}
        autoHideDuration={3000}
        onClose={() => setSnackbar({ ...snackbar, open: false })}
      >
        <Alert severity={snackbar.severity}>{snackbar.message}</Alert>
      </Snackbar>
    </Box>
  );
}
